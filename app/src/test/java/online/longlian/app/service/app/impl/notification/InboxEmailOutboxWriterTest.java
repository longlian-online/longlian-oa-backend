package online.longlian.app.service.app.impl.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.pojo.entity.UserNotification;
import online.longlian.app.service.outbox.OutboxService;
import online.longlian.common.enumeration.InboxNotificationKind;
import online.longlian.common.enumeration.OutboxMessageStatus;
import online.longlian.common.enumeration.OutboxMessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxEmailOutboxWriterTest {
    @Mock OutboxService outboxService;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldEnqueueOneInboxEmailPerNotification() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 23, 8, 0);
        InboxEmailOutboxWriter writer = new InboxEmailOutboxWriter(outboxService, objectMapper);
        writer.enqueue(8L, List.of(
                notification(11L, 4L, InboxNotificationKind.TASK_READY, 202L, createdAt),
                notification(12L, 3L, InboxNotificationKind.ITEM_COMPLETED, 100L, createdAt.plusMinutes(1))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OutboxMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxService).enqueueAll(captor.capture());
        List<OutboxMessage> messages = captor.getValue();
        assertThat(messages).hasSize(2);
        assertMessage(messages.get(0), "11", createdAt, 11L, 8L, 4L, "TASK_READY", 202L);
        assertMessage(messages.get(1), "12", createdAt.plusMinutes(1), 12L, 8L, 3L, "ITEM_COMPLETED", 100L);
    }

    @Test
    void shouldNotSwallowPayloadSerializationFailure() throws Exception {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.writeValueAsString(any())).thenThrow(new JsonProcessingException("bad") {
        });
        InboxEmailOutboxWriter writer = new InboxEmailOutboxWriter(outboxService, failing);

        assertThatThrownBy(() -> writer.enqueue(1L, List.of(
                notification(11L, 4L, InboxNotificationKind.TASK_READY, 202L, LocalDateTime.of(2026, 10, 23, 8, 0)))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("站内通知邮件 payload 序列化失败");
        verifyNoInteractions(outboxService);
    }

    private void assertMessage(OutboxMessage message, String dedupeKey, LocalDateTime createdAt, long notificationId,
                               long orgId, long recipientUserId, String kind, long sourceId) throws Exception {
        assertThat(message.getType()).isEqualTo(OutboxMessageType.INBOX_EMAIL);
        assertThat(message.getDedupeKey()).isEqualTo(dedupeKey);
        assertThat(message.getStatus()).isEqualTo(OutboxMessageStatus.PENDING);
        assertThat(message.getAttemptCount()).isZero();
        assertThat(message.getAvailableAt()).isEqualTo(createdAt);
        JsonNode payload = objectMapper.readTree(message.getPayload());
        assertThat(payload.get("notificationId").asLong()).isEqualTo(notificationId);
        assertThat(payload.get("orgId").asLong()).isEqualTo(orgId);
        assertThat(payload.get("recipientUserId").asLong()).isEqualTo(recipientUserId);
        assertThat(payload.get("kind").asText()).isEqualTo(kind);
        assertThat(payload.get("sourceId").asLong()).isEqualTo(sourceId);
    }

    private static UserNotification notification(long id, long recipientUserId, InboxNotificationKind kind,
                                                 long sourceId, LocalDateTime createdAt) {
        return UserNotification.builder().id(id).orgId(1L).recipientUserId(recipientUserId).kind(kind)
                .sourceId(sourceId).createdAt(createdAt).build();
    }
}
