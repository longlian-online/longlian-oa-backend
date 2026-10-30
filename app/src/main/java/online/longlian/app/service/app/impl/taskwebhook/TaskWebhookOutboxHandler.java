package online.longlian.app.service.app.impl.taskwebhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
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
import online.longlian.app.service.app.impl.taskinstance.SlotLabels;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.service.outbox.OutboxHandler;
import online.longlian.app.service.outbox.OutboxPermanentException;
import online.longlian.app.service.outbox.OutboxSkipException;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.OutboxMessageType;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TaskWebhookOutboxHandler implements OutboxHandler {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TASK_SUBMIT = "task_submit";

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TaskWebhookAddressGuard addressGuard;
    private final TaskWebhookClient client;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final ItemMapper itemMapper;
    private final ProjectMapper projectMapper;
    private final UserService userService;
    private final ResourceService resourceService;
    private final TaskFormService taskFormService;

    @Override
    public OutboxMessageType type() {
        return OutboxMessageType.TASK_WEBHOOK;
    }

    @Override
    public void deliver(OutboxMessage message, LocalDateTime executeTime) {
        TaskWebhookDispatch dispatch = read(message.getPayload());
        ItemTaskNode node = itemTaskNodeMapper.selectById(dispatch.itemTaskNodeId());
        if (node == null || node.getWebhookUrl() == null || node.getWebhookUrl().isBlank()) {
            throw new OutboxSkipException("未配置 webhook");
        }
        if (node.getWebhookSecret() == null || node.getWebhookSecret().isBlank()) {
            throw new OutboxPermanentException("webhook 密钥缺失");
        }
        TaskSubmission submission = taskSubmissionMapper.selectById(dispatch.submissionId());
        if (submission == null || !submission.getTaskInstanceId().equals(dispatch.taskInstanceId())
                || !submission.getItemTaskNodeId().equals(dispatch.itemTaskNodeId())) {
            throw new OutboxSkipException("提交不存在");
        }
        Item item = itemMapper.selectById(submission.getItemId());
        Project project = item == null ? null : projectMapper.selectById(item.getProjectId());
        if (project == null || !Long.valueOf(dispatch.orgId()).equals(project.getOrgId())) {
            throw new OutboxSkipException("提交不存在");
        }
        byte[] body = body(message.getId(), dispatch, node, submission, item, project);
        String timestamp = Long.toString(clock.instant().getEpochSecond());
        String deliveryId = Long.toString(message.getId());
        int status;
        try {
            status = client.post(addressGuard.require(node.getWebhookUrl()), body, timestamp, deliveryId,
                    signature(node.getWebhookSecret(), timestamp, body));
        } catch (IllegalArgumentException exception) {
            throw new OutboxPermanentException(exception.getMessage());
        }
        if (status >= 200 && status < 300) {
            return;
        }
        if (status == 408 || status == 429 || status >= 500) {
            throw new TaskWebhookTransientException("webhook 响应 " + status);
        }
        throw new OutboxPermanentException("webhook 响应 " + status);
    }

    private byte[] body(Long messageId, TaskWebhookDispatch dispatch, ItemTaskNode node, TaskSubmission submission,
                        Item item, Project project) {
        List<TaskFormField> fields;
        TaskFormService.ValidatedValues values;
        try {
            fields = taskFormService.parseFields(node.getMetaSchema());
            values = taskFormService.parseValues(fields, submission.getMetadata());
        } catch (AppException exception) {
            throw new OutboxPermanentException("提交内容无法读取");
        }
        Map<Long, ActivatedResourceRead> reads = reads(dispatch, values.resourceIds());
        List<Map<String, Object>> fieldBodies = new ArrayList<>();
        List<Map<String, Object>> attachments = new ArrayList<>();
        for (TaskFormField field : fields) {
            Object raw = values.values().get(field.key());
            if ("file".equals(field.type())) {
                String fileId = fileId(raw);
                fieldBodies.add(fieldBody(field, fileId));
                if (fileId != null) {
                    attachments.add(attachment(fileId, reads.get(Long.valueOf(fileId))));
                }
            } else {
                fieldBodies.add(fieldBody(field, raw == null ? null : raw.toString()));
            }
        }
        List<ItemStageNodeStatusBO> stages = taskInstanceMapper.selectItemStageNodeStatuses(List.of(item.getId()));
        String slot = slotText(node.getName(), SlotLabels.of(node.getId(), stages));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", "evt_" + messageId);
        body.put("type", dispatch.event());
        body.put("occurredAt", dispatch.occurredAt());
        body.put("task", Map.of("id", Long.toString(dispatch.taskInstanceId()), "revision", dispatch.revision(),
                "title", item.getTitle() + " · " + slot));
        Map<String, Object> submissionBody = new LinkedHashMap<>();
        submissionBody.put("id", Long.toString(submission.getId()));
        submissionBody.put("version", submission.getVersionNo());
        body.put("submission", submissionBody);
        body.put("project", Map.of("id", Long.toString(project.getId()), "name", project.getTitle()));
        body.put("item", Map.of("id", Long.toString(item.getId()), "name", item.getTitle()));
        body.put("submitter", submitter(submission.getSubmitterId()));
        body.put("fields", fieldBodies);
        body.put("attachments", attachments);
        if (dispatch.reason() != null) {
            body.put("reason", dispatch.reason());
        }
        if (TaskWebhookDispatch.INVALIDATED.equals(dispatch.event()) && submission.getReviewComment() != null
                && !submission.getReviewComment().isBlank()) {
            body.put("reviewComment", submission.getReviewComment());
        }
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException exception) {
            throw new OutboxPermanentException("webhook 正文无法生成");
        }
    }

    private Map<Long, ActivatedResourceRead> reads(TaskWebhookDispatch dispatch, List<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ActivatedResource> matched = resourceService.findActivated(
                TASK_SUBMIT, dispatch.taskInstanceId(), dispatch.orgId(), resourceIds);
        return matched.isEmpty() ? Map.of() : resourceService.signActivated(matched.values());
    }

    private Map<String, Object> submitter(Long userId) {
        User user = userService.getById(userId);
        String name = user == null ? null : user.getNickname();
        Map<String, Object> submitter = new LinkedHashMap<>();
        submitter.put("id", Long.toString(userId));
        submitter.put("name", name == null || name.isBlank() ? null : name);
        return submitter;
    }

    private Map<String, Object> attachment(String fileId, ActivatedResourceRead read) {
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("fileId", fileId);
        if (read == null) {
            attachment.put("name", "附件不可用");
            return attachment;
        }
        attachment.put("name", read.fileName());
        attachment.put("size", read.fileSize());
        attachment.put("contentType", read.fileMime());
        attachment.put("url", read.readUrl());
        attachment.put("urlExpiresAt", LocalDateTime.ofInstant(Instant.ofEpochSecond(read.expiresAt()), clock.getZone())
                .format(TIMESTAMP));
        return attachment;
    }

    private static Map<String, Object> fieldBody(TaskFormField field, String value) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("key", field.key());
        body.put("label", field.label());
        body.put("type", field.type());
        body.put("value", value);
        return body;
    }

    private static String fileId(Object raw) {
        if (!(raw instanceof Map<?, ?> file) || !(file.get("fileId") instanceof String id)) {
            return null;
        }
        return id;
    }

    private static String slotText(String name, SlotLabels.Position position) {
        String named = position.serialStep() != null && position.serialStep() > 0
                ? name + "（第 " + position.serialStep() + " 步）"
                : name;
        if (position.slotTotal() > 1) {
            return named + " · " + position.slotIndex() + "/" + position.slotTotal();
        }
        return named;
    }

    private TaskWebhookDispatch read(String payload) {
        try {
            TaskWebhookDispatch dispatch = objectMapper.readValue(payload, TaskWebhookDispatch.class);
            if (dispatch == null || dispatch.event() == null) {
                throw new OutboxPermanentException("webhook payload 无效");
            }
            return dispatch;
        } catch (JsonProcessingException exception) {
            throw new OutboxPermanentException("webhook payload 无效");
        }
    }

    private static String signature(String secret, String timestamp, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            return "v1=" + HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException exception) { // skipcq: TCV-001
            throw new IllegalStateException("无法生成 webhook 签名", exception); // skipcq: TCV-001
        }
    }
}
