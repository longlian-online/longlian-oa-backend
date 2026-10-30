package online.longlian.app.service.app.impl.taskwebhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.service.outbox.OutboxService;
import online.longlian.common.enumeration.OutboxMessageStatus;
import online.longlian.common.enumeration.OutboxMessageType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskWebhookOutboxWriterTest {
    private final ItemTaskNodeMapper nodes = mock(ItemTaskNodeMapper.class);
    private final OutboxService outboxService = mock(OutboxService.class);
    private final TaskWebhookOutboxWriter writer = new TaskWebhookOutboxWriter(nodes, outboxService, new ObjectMapper());
    private final LocalDateTime enqueuedAt = LocalDateTime.of(2026, 10, 30, 12, 0);

    @Test
    void shouldSkipWhenNodeHasNoUrl() {
        when(nodes.selectById(4L)).thenReturn(ItemTaskNode.builder().id(4L).build());

        writer.enqueue(dispatch(TaskWebhookDispatch.SUBMITTED, null), enqueuedAt);

        verify(outboxService, never()).enqueueAll(anyList());
    }

    @Test
    void shouldEnqueueSubmissionAndInvalidationUnderSeparateKeys() throws Exception {
        when(nodes.selectById(4L)).thenReturn(ItemTaskNode.builder().id(4L).webhookUrl("https://example.com/hook").build());

        writer.enqueue(dispatch(TaskWebhookDispatch.SUBMITTED, null), enqueuedAt);
        writer.enqueue(dispatch(TaskWebhookDispatch.INVALIDATED, TaskWebhookDispatch.RESET), enqueuedAt);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(outboxService, org.mockito.Mockito.times(2)).enqueueAll(captor.capture());
        OutboxMessage submitted = (OutboxMessage) captor.getAllValues().get(0).get(0);
        OutboxMessage invalidated = (OutboxMessage) captor.getAllValues().get(1).get(0);
        assertThat(submitted.getType()).isEqualTo(OutboxMessageType.TASK_WEBHOOK);
        assertThat(submitted.getStatus()).isEqualTo(OutboxMessageStatus.PENDING);
        assertThat(submitted.getDedupeKey()).isEqualTo("submitted:9");
        assertThat(submitted.getPayload()).doesNotContain("secret").contains("\"orgId\":1");
        assertThat(invalidated.getDedupeKey()).isEqualTo("invalidated:9");
        assertThat(new ObjectMapper().readTree(invalidated.getPayload()).get("reason").asText()).isEqualTo("reset");
    }

    private static TaskWebhookDispatch dispatch(String event, String reason) {
        return new TaskWebhookDispatch(1L, event, 8L, 9L, 4L, 3L, "2026-10-30 11:00:00", reason);
    }
}
