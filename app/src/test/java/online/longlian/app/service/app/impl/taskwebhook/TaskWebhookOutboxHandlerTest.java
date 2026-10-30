package online.longlian.app.service.app.impl.taskwebhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.common.ActivatedResource;
import online.longlian.app.pojo.bo.common.ActivatedResourceRead;
import online.longlian.app.pojo.bo.common.TaskFormField;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.app.UserService;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.service.outbox.OutboxPermanentException;
import online.longlian.app.service.outbox.OutboxSkipException;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.OutboxMessageType;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskWebhookOutboxHandlerTest {
    private static final String SECRET = "0123456789abcdef";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T04:00:00Z"), ZoneId.of("Asia/Shanghai"));

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TaskWebhookAddressGuard guard = mock(TaskWebhookAddressGuard.class);
    private final TaskWebhookClient client = mock(TaskWebhookClient.class);
    private final ItemTaskNodeMapper nodes = mock(ItemTaskNodeMapper.class);
    private final TaskSubmissionMapper submissions = mock(TaskSubmissionMapper.class);
    private final TaskInstanceMapper instances = mock(TaskInstanceMapper.class);
    private final ItemMapper items = mock(ItemMapper.class);
    private final ProjectMapper projects = mock(ProjectMapper.class);
    private final UserService users = mock(UserService.class);
    private final ResourceService resources = mock(ResourceService.class);
    private TaskWebhookOutboxHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TaskWebhookOutboxHandler(objectMapper, CLOCK, guard, client, nodes, submissions, instances,
                items, projects, users, resources, new TaskFormService());
        when(guard.require("https://example.com/hook")).thenReturn(URI.create("https://example.com/hook"));
    }

    @Test
    void shouldSignTheSubmittedBodyAndIncludeAFreshAttachmentUrl() throws Exception {
        stubSubmission();
        when(client.post(any(), any(), anyString(), anyString(), anyString())).thenReturn(204);
        when(resources.findActivated(eq("task_submit"), eq(8L), eq(1L), eq(List.of(5L))))
                .thenReturn(Map.of(5L, new ActivatedResource(5L, "a.zip", 1024L, "application/zip",
                        StorageType.OSS, "task_submit/5.zip", 1L)));
        when(resources.signActivated(anyCollection())).thenReturn(Map.of(5L, new ActivatedResourceRead(
                5L, "a.zip", 1024L, "application/zip", "https://cdn.example/a.zip", 1793333100L)));

        handler.deliver(message(), LocalDateTime.of(2026, 10, 30, 12, 0));

        ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<String> timestamp = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> signature = ArgumentCaptor.forClass(String.class);
        verify(client).post(eq(URI.create("https://example.com/hook")), body.capture(), timestamp.capture(),
                eq("41"), signature.capture());
        assertThat(timestamp.getValue()).isEqualTo("1793332800");
        assertThat(signature.getValue()).isEqualTo(sign(timestamp.getValue(), body.getValue()));
        JsonNode json = objectMapper.readTree(body.getValue());
        assertThat(json.get("id").asText()).isEqualTo("evt_41");
        assertThat(json.get("type").asText()).isEqualTo("task.submitted");
        assertThat(json.get("task").get("title").asText()).isEqualTo("第一话 · 翻译");
        assertThat(json.get("task").get("id").asText()).isEqualTo("8");
        assertThat(json.get("submitter").get("name").asText()).isEqualTo("小明");
        assertThat(json.get("fields").get(0).get("value").asText()).isEqualTo("你好");
        assertThat(json.get("attachments").get(0).get("url").asText()).isEqualTo("https://cdn.example/a.zip");
        assertThat(json.get("attachments").get(0).get("urlExpiresAt").asText()).isEqualTo("2026-10-30 12:05:00");
        assertThat(json.has("reason")).isFalse();
    }

    @Test
    void shouldSkipWhenTheNodeHasNoUrl() throws Exception {
        when(nodes.selectById(4L)).thenReturn(ItemTaskNode.builder().id(4L).build());

        assertThatThrownBy(() -> handler.deliver(message(), LocalDateTime.now()))
                .isInstanceOf(OutboxSkipException.class);
        verify(client, never()).post(any(), any(), any(), any(), any());
    }

    @Test
    void shouldStopRetryingAClientError() throws Exception {
        stubSubmission();
        when(client.post(any(), any(), any(), any(), any())).thenReturn(400);

        assertThatThrownBy(() -> handler.deliver(message(), LocalDateTime.now()))
                .isInstanceOf(OutboxPermanentException.class)
                .hasMessage("webhook 响应 400");
    }

    @Test
    void shouldRetryAServerError() throws Exception {
        stubSubmission();
        when(client.post(any(), any(), any(), any(), any())).thenReturn(503);

        assertThatThrownBy(() -> handler.deliver(message(), LocalDateTime.now()))
                .isInstanceOf(TaskWebhookTransientException.class)
                .hasMessage("webhook 响应 503");
    }

    @Test
    void shouldSkipWhenSubmissionDoesNotMatchTheNode() throws Exception {
        when(nodes.selectById(4L)).thenReturn(ItemTaskNode.builder().id(4L)
                .webhookUrl("https://example.com/hook").webhookSecret(SECRET).build());
        when(submissions.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> handler.deliver(message(), LocalDateTime.now()))
                .isInstanceOf(OutboxSkipException.class);
    }

    @Test
    void shouldStopWhenTheSecretIsMissing() throws Exception {
        when(nodes.selectById(4L)).thenReturn(ItemTaskNode.builder().id(4L)
                .webhookUrl("https://example.com/hook").build());

        assertThatThrownBy(() -> handler.deliver(message(), LocalDateTime.now()))
                .isInstanceOf(OutboxPermanentException.class)
                .hasMessage("webhook 密钥缺失");
    }

    @Test
    void shouldStopWhenTheAddressIsRejected() throws Exception {
        stubSubmission();
        when(guard.require(anyString())).thenThrow(new IllegalArgumentException("webhook 地址不能指向内网"));

        assertThatThrownBy(() -> handler.deliver(message(), LocalDateTime.now()))
                .isInstanceOf(OutboxPermanentException.class)
                .hasMessage("webhook 地址不能指向内网");
    }

    @Test
    void shouldIncludeTheInvalidationReasonAndUnavailableAttachment() throws Exception {
        stubSubmission();
        when(submissions.selectById(9L)).thenReturn(TaskSubmission.builder().id(9L).taskInstanceId(8L)
                .itemTaskNodeId(4L).itemId(3L).submitterId(7L).versionNo(2)
                .reviewComment("重做")
                .metadata("{\"note\":\"你好\",\"pack\":{\"fileId\":\"5\"}}").build());
        when(resources.findActivated(anyString(), anyLong(), anyLong(), any())).thenReturn(Map.of());
        when(client.post(any(), any(), any(), any(), any())).thenReturn(204);
        OutboxMessage invalidated = OutboxMessage.builder().id(42L).type(OutboxMessageType.TASK_WEBHOOK)
                .payload(objectMapper.writeValueAsString(new TaskWebhookDispatch(
                        1L, TaskWebhookEvents.INVALIDATED, 8L, 9L, 4L, 4L,
                        "2026-10-30 12:10:00", TaskWebhookEvents.REJECTED)))
                .build();

        handler.deliver(invalidated, LocalDateTime.now());

        ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
        verify(client).post(any(), body.capture(), any(), eq("42"), any());
        JsonNode json = objectMapper.readTree(body.getValue());
        assertThat(json.get("reason").asText()).isEqualTo("rejected");
        assertThat(json.get("reviewComment").asText()).isEqualTo("重做");
        assertThat(json.get("attachments").get(0).get("name").asText()).isEqualTo("附件不可用");
        assertThat(json.get("attachments").get(0).has("url")).isFalse();
    }

    private void stubSubmission() {
        when(nodes.selectById(4L)).thenReturn(ItemTaskNode.builder().id(4L).name("翻译")
                .webhookUrl("https://example.com/hook").webhookSecret(SECRET)
                .metaSchema(new TaskFormService().serializeFields(List.of(
                        new TaskFormField("note", "备注", "text", true, List.of()),
                        new TaskFormField("pack", "文件", "file", true, List.of()))))
                .build());
        when(submissions.selectById(9L)).thenReturn(TaskSubmission.builder().id(9L).taskInstanceId(8L)
                .itemTaskNodeId(4L).itemId(3L).submitterId(7L).versionNo(2)
                .metadata("{\"note\":\"你好\",\"pack\":{\"fileId\":\"5\"}}").build());
        when(items.selectById(3L)).thenReturn(Item.builder().id(3L).projectId(2L).title("第一话").build());
        when(projects.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(1L).title("企划").build());
        when(users.getById(7L)).thenReturn(User.builder().id(7L).nickname("小明").build());
        when(instances.selectItemStageNodeStatuses(List.of(3L))).thenReturn(List.of(
                new ItemStageNodeStatusBO(3L, 4L, "翻译", 1, 1, true)));
    }

    private OutboxMessage message() throws Exception {
        return OutboxMessage.builder().id(41L).type(OutboxMessageType.TASK_WEBHOOK)
                .payload(objectMapper.writeValueAsString(new TaskWebhookDispatch(
                        1L, TaskWebhookEvents.SUBMITTED, 8L, 9L, 4L, 3L, "2026-10-30 11:00:00", null)))
                .build();
    }

    private static String sign(String timestamp, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
        return "v1=" + HexFormat.of().formatHex(mac.doFinal(body));
    }
}
