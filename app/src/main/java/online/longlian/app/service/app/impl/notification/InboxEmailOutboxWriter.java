package online.longlian.app.service.app.impl.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.pojo.entity.UserNotification;
import online.longlian.app.service.outbox.OutboxService;
import online.longlian.common.enumeration.OutboxMessageStatus;
import online.longlian.common.enumeration.OutboxMessageType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InboxEmailOutboxWriter {
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public void enqueue(Long orgId, List<UserNotification> notifications) {
        List<OutboxMessage> messages = new ArrayList<>(notifications.size());
        for (UserNotification notification : notifications) {
            messages.add(OutboxMessage.builder()
                    .type(OutboxMessageType.INBOX_EMAIL)
                    .dedupeKey(Long.toString(notification.getId()))
                    .payload(payload(orgId, notification))
                    .status(OutboxMessageStatus.PENDING)
                    .attemptCount(0)
                    .availableAt(notification.getCreatedAt())
                    .createdAt(notification.getCreatedAt())
                    .updatedAt(notification.getCreatedAt())
                    .build());
        }
        outboxService.enqueueAll(messages);
    }

    private String payload(Long orgId, UserNotification notification) {
        try {
            return objectMapper.writeValueAsString(new InboxEmailPayload(
                    notification.getId(),
                    orgId,
                    notification.getRecipientUserId(),
                    notification.getKind(),
                    notification.getSourceId()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("站内通知邮件 payload 序列化失败", exception);
        }
    }
}
