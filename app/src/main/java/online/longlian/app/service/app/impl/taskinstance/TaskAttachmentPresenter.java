package online.longlian.app.service.app.impl.taskinstance;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.app.project.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.common.ActivatedResourceResultBO;
import online.longlian.app.pojo.bo.common.ActivatedResourceReadResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskAttachmentResultBO;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 把已授权的任务文件读取结果组装成详情卡片。资源存储不感知这个展示结构。 */
@Component
@RequiredArgsConstructor
public class TaskAttachmentPresenter {

    private static final String TASK_SUBMIT = "task_submit";

    private final ResourceService resourceService;

    public Map<Long, TaskAttachmentResultBO> present(Long taskId, Long orgId, List<Long> resourceIds) {
        Map<Long, TaskAttachmentResultBO> attachments = new HashMap<>();
        if (resourceIds.isEmpty()) {
            return attachments;
        }
        Map<Long, ActivatedResourceResultBO> matched = resourceService.findActivated(TASK_SUBMIT, taskId, orgId, resourceIds);
        for (Long resourceId : resourceIds) {
            ActivatedResourceResultBO resource = matched.get(resourceId);
            TaskAttachmentResultBO attachment = new TaskAttachmentResultBO();
            attachment.setId(resourceId.toString());
            attachment.setAvailability("unavailable");
            attachment.setName("附件不可用");
            if (resource != null) {
                attachment.setName(resource.fileName());
                attachment.setSizeText(sizeText(resource.fileSize()));
                attachment.setMediaType(mediaType(resource.fileMime()));
                attachment.setAvailability("available");
            }
            attachments.put(resourceId, attachment);
        }
        return attachments;
    }

    /** 点击预览或下载时签发。详情和版本列表不带读取链接。 */
    public ProjectAttachmentDownloadResultBO download(Long taskId, Long orgId, Long resourceId) {
        ActivatedResourceResultBO resource = resourceService
                .findActivated(TASK_SUBMIT, taskId, orgId, List.of(resourceId))
                .get(resourceId);
        if (resource == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "附件不存在");
        }
        ActivatedResourceReadResultBO read = resourceService.signDownload(resource);
        return ProjectAttachmentDownloadResultBO.builder()
                .readUrl(read.readUrl())
                .expireAt(read.expiresAt())
                .build();
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
