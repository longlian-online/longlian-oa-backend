package online.longlian.app.service.app.impl.taskinstance;

import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.common.ActivatedResource;
import online.longlian.app.pojo.bo.common.ActivatedResourceRead;
import online.longlian.app.pojo.vo.app.TaskAttachmentVO;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Turns authorized task-file reads into detail cards. Resource storage stays unaware of this view. */
@Component
@RequiredArgsConstructor
public class TaskAttachmentPresenter {

    private static final String TASK_SUBMIT = "task_submit";

    private final ResourceService resourceService;

    public Map<Long, TaskAttachmentVO> present(Long taskId, Long orgId, List<Long> resourceIds) {
        Map<Long, TaskAttachmentVO> attachments = new HashMap<>();
        if (resourceIds.isEmpty()) {
            return attachments;
        }
        Map<Long, ActivatedResource> matched = resourceService.findActivated(TASK_SUBMIT, taskId, orgId, resourceIds);
        if (!matched.isEmpty() && !resourceService.cdnEnabled()) {
            throw new IllegalStateException("任务附件读取必须启用 CDN");
        }
        Map<Long, ActivatedResourceRead> signed = matched.isEmpty()
                ? Map.of()
                : resourceService.signActivated(matched.values());
        for (Long resourceId : resourceIds) {
            ActivatedResourceRead read = signed.get(resourceId);
            TaskAttachmentVO attachment = new TaskAttachmentVO();
            attachment.setId(resourceId.toString());
            attachment.setAvailability("unavailable");
            attachment.setName("附件不可用");
            if (read != null) {
                attachment.setName(read.fileName());
                attachment.setReadUrl(read.readUrl());
                attachment.setExpiresAt(read.expiresAt());
                attachment.setSizeText(sizeText(read.fileSize()));
                attachment.setMediaType(mediaType(read.fileMime()));
                attachment.setAvailability("available");
            }
            attachments.put(resourceId, attachment);
        }
        return attachments;
    }

    private String sizeText(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format(Locale.ROOT, "%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String mediaType(String mime) {
        String normalized = mime.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("image/")) {
            return "image";
        }
        return switch (normalized) {
            case "application/pdf", "application/msword", "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "document";
            case "application/zip", "application/x-zip-compressed" -> "archive";
            default -> "other";
        };
    }
}
