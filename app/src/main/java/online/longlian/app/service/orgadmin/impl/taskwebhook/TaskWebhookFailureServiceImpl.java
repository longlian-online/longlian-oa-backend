package online.longlian.app.service.orgadmin.impl.taskwebhook;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskWebhookFailureResultBO;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.service.app.impl.taskwebhook.TaskWebhookDispatch;
import online.longlian.app.service.orgadmin.TaskWebhookFailureService;
import online.longlian.app.service.outbox.OutboxService;
import online.longlian.common.enumeration.OutboxMessageStatus;
import online.longlian.common.enumeration.OutboxMessageType;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskWebhookFailureServiceImpl implements TaskWebhookFailureService {
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public PageResultBO<TaskWebhookFailureResultBO> listFailures(Long orgId, int pageNum, int pageSize) {
        Page<OutboxMessage> page = outboxService.listScoped(
                OutboxMessageType.TASK_WEBHOOK, OutboxMessageStatus.FAILED, orgId, pageNum, pageSize);
        return new PageResultBO<>(page.getRecords().stream().map(this::toResult).toList(), page.getTotal());
    }

    @Override
    public void replay(Long orgId, Long messageId) {
        boolean replayed = outboxService.replayFailed(
                OutboxMessageType.TASK_WEBHOOK, orgId, messageId, LocalDateTime.now(clock));
        if (!replayed) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "投递记录不存在");
        }
    }

    private TaskWebhookFailureResultBO toResult(OutboxMessage message) {
        TaskWebhookDispatch dispatch = read(message.getPayload());
        return TaskWebhookFailureResultBO.builder()
                .id(message.getId())
                .event(dispatch == null ? null : dispatch.event())
                .taskInstanceId(dispatch == null ? null : dispatch.taskInstanceId())
                .submissionId(dispatch == null ? null : dispatch.submissionId())
                .lastError(message.getLastError())
                .attemptCount(message.getAttemptCount())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    private TaskWebhookDispatch read(String payload) {
        try {
            return objectMapper.readValue(payload, TaskWebhookDispatch.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
