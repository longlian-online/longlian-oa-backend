package online.longlian.app.service.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.mapper.OutboxMessageMapper;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.common.enumeration.OutboxMessageStatus;
import online.longlian.common.enumeration.OutboxMessageType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OutboxDispatcher {
    private static final int BATCH_SIZE = 20;
    private static final int LEASE_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int ERROR_LIMIT = 500;

    private final Map<OutboxMessageType, OutboxHandler> handlers;
    private final OutboxMessageMapper mapper;
    private final TransactionTemplate transactionTemplate;

    public OutboxDispatcher(List<OutboxHandler> handlers, OutboxMessageMapper mapper,
                            PlatformTransactionManager transactionManager) {
        this.handlers = Map.copyOf(index(handlers));
        this.mapper = mapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void dispatch(LocalDateTime executeTime) {
        Page<OutboxMessage> selected = mapper.selectPage(new Page<>(1, BATCH_SIZE, false), due(executeTime));
        List<OutboxMessage> records = selected == null ? List.of() : selected.getRecords();
        if (records == null || records.isEmpty()) {
            return;
        }
        for (OutboxMessage message : records) {
            try {
                dispatchOne(message, executeTime);
            } catch (RuntimeException exception) {
                log.warn("outbox 单条投递未完成 id={} type={}", message.getId(), message.getType(), exception);
            }
        }
    }

    private void dispatchOne(OutboxMessage message, LocalDateTime executeTime) {
        Boolean claimed = transactionTemplate.execute(status -> claim(message, executeTime));
        if (!Boolean.TRUE.equals(claimed)) {
            return;
        }
        Outcome outcome = deliver(message, executeTime);
        transactionTemplate.executeWithoutResult(status -> finish(message, executeTime, outcome));
    }

    private boolean claim(OutboxMessage message, LocalDateTime executeTime) {
        int attempt = message.getAttemptCount() + 1;
        int updated = mapper.update(null, new LambdaUpdateWrapper<OutboxMessage>()
                .set(OutboxMessage::getStatus, OutboxMessageStatus.PROCESSING)
                .set(OutboxMessage::getAttemptCount, attempt)
                .set(OutboxMessage::getLockedUntil, executeTime.plusMinutes(LEASE_MINUTES))
                .set(OutboxMessage::getUpdatedAt, executeTime)
                .eq(OutboxMessage::getId, message.getId())
                .eq(OutboxMessage::getStatus, message.getStatus())
                .eq(OutboxMessage::getAttemptCount, message.getAttemptCount()));
        if (updated == 0) {
            return false;
        }
        message.setStatus(OutboxMessageStatus.PROCESSING);
        message.setAttemptCount(attempt);
        message.setLockedUntil(executeTime.plusMinutes(LEASE_MINUTES));
        return true;
    }

    private Outcome deliver(OutboxMessage message, LocalDateTime executeTime) {
        OutboxMessageType type = message.getType();
        OutboxHandler handler = type == null ? null : handlers.get(type);
        if (handler == null) {
            return new Outcome(OutboxMessageStatus.FAILED, "没有对应的 outbox 处理器", null);
        }
        try {
            handler.deliver(message, executeTime);
            return new Outcome(OutboxMessageStatus.SENT, null, null);
        } catch (OutboxSkipException exception) {
            return new Outcome(OutboxMessageStatus.SKIPPED, errorText(exception), null);
        } catch (OutboxPermanentException exception) {
            return new Outcome(OutboxMessageStatus.FAILED, errorText(exception), null);
        } catch (RuntimeException exception) {
            String error = errorText(exception);
            if (message.getAttemptCount() >= MAX_ATTEMPTS) {
                return new Outcome(OutboxMessageStatus.FAILED, error, null);
            }
            return new Outcome(OutboxMessageStatus.PENDING, error, retryAt(message.getAttemptCount(), executeTime));
        }
    }

    private void finish(OutboxMessage message, LocalDateTime executeTime, Outcome outcome) {
        LambdaUpdateWrapper<OutboxMessage> update = new LambdaUpdateWrapper<OutboxMessage>()
                .set(OutboxMessage::getStatus, outcome.status())
                .set(OutboxMessage::getLockedUntil, null)
                .set(OutboxMessage::getLastError, outcome.lastError())
                .set(OutboxMessage::getUpdatedAt, executeTime)
                .eq(OutboxMessage::getId, message.getId())
                .eq(OutboxMessage::getStatus, OutboxMessageStatus.PROCESSING);
        if (outcome.availableAt() != null) {
            update.set(OutboxMessage::getAvailableAt, outcome.availableAt());
        }
        mapper.update(null, update);
    }

    private static LambdaQueryWrapper<OutboxMessage> due(LocalDateTime executeTime) {
        return new LambdaQueryWrapper<OutboxMessage>()
                .and(group -> group
                        .eq(OutboxMessage::getStatus, OutboxMessageStatus.PENDING)
                        .le(OutboxMessage::getAvailableAt, executeTime)
                        .or(processing -> processing
                                .eq(OutboxMessage::getStatus, OutboxMessageStatus.PROCESSING)
                                .le(OutboxMessage::getLockedUntil, executeTime)))
                .orderByAsc(OutboxMessage::getId);
    }

    private static LocalDateTime retryAt(int attempt, LocalDateTime executeTime) {
        int minutes = switch (attempt) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 15;
            case 4 -> 60;
            default -> throw new IllegalStateException("outbox 重试次数无效");
        };
        return executeTime.plusMinutes(minutes);
    }

    private static String errorText(Throwable error) {
        String message = error.getMessage();
        String text = message == null ? error.getClass().getSimpleName() : message;
        if (text == null || text.isEmpty()) {
            text = error.getClass().getName();
        }
        return text.length() <= ERROR_LIMIT ? text : text.substring(0, ERROR_LIMIT);
    }

    private static Map<OutboxMessageType, OutboxHandler> index(List<OutboxHandler> handlers) {
        Map<OutboxMessageType, OutboxHandler> indexed = new EnumMap<>(OutboxMessageType.class);
        for (OutboxHandler handler : handlers) {
            OutboxMessageType type = handler.type();
            if (type == null) {
                throw new IllegalStateException("outbox 处理器缺少类型");
            }
            if (indexed.put(type, handler) != null) {
                throw new IllegalStateException("outbox 类型 " + type + " 存在多个处理器");
            }
        }
        for (OutboxMessageType type : OutboxMessageType.values()) {
            if (!indexed.containsKey(type)) {
                throw new IllegalStateException("outbox 类型 " + type + " 没有处理器");
            }
        }
        return indexed;
    }

    private static final class Outcome {
        private final OutboxMessageStatus status;
        private final String lastError;
        private final LocalDateTime availableAt;

        private Outcome(OutboxMessageStatus status, String lastError, LocalDateTime availableAt) {
            this.status = status;
            this.lastError = lastError;
            this.availableAt = availableAt;
        }

        private OutboxMessageStatus status() {
            return status;
        }

        private String lastError() {
            return lastError;
        }

        private LocalDateTime availableAt() {
            return availableAt;
        }
    }
}
