package online.longlian.app.service.app.impl.taskwebhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.service.outbox.OutboxService;
import online.longlian.common.enumeration.OutboxMessageStatus;
import online.longlian.common.enumeration.OutboxMessageType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** 没配地址就不记 outbox。调用方必须已经处在提交事务里，这样投递记录和提交一起提交或回滚。 */
@Component
@RequiredArgsConstructor
public class TaskWebhookOutboxWriter {
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public void enqueue(TaskWebhookDispatch dispatch, LocalDateTime enqueuedAt) {
        ItemTaskNode node = itemTaskNodeMapper.selectById(dispatch.itemTaskNodeId());
        if (node == null || node.getWebhookUrl() == null || node.getWebhookUrl().isBlank()) {
            return;
        }
        outboxService.enqueueAll(List.of(OutboxMessage.builder()
                .type(OutboxMessageType.TASK_WEBHOOK)
                .dedupeKey(dedupeKey(dispatch))
                .payload(payload(dispatch))
                .status(OutboxMessageStatus.PENDING)
                .attemptCount(0)
                .availableAt(enqueuedAt)
                .createdAt(enqueuedAt)
                .updatedAt(enqueuedAt)
                .build()));
    }

    private static String dedupeKey(TaskWebhookDispatch dispatch) {
        String prefix = TaskWebhookDispatch.SUBMITTED.equals(dispatch.event()) ? "submitted:" : "invalidated:";
        return prefix + dispatch.submissionId();
    }

    private String payload(TaskWebhookDispatch dispatch) {
        try {
            return objectMapper.writeValueAsString(dispatch);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("任务 webhook payload 序列化失败", exception);
        }
    }
}
