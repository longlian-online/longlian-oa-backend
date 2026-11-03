package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectAttachmentMapper;
import online.longlian.app.pojo.bo.common.ResourceAbandonParamsBO;
import online.longlian.app.pojo.entity.ProjectAttachment;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 企划附件关联的删除处理：逻辑删除关联并把文件送入资源保留期。
 * <p>
 * 供单附件删除与企划整体删除共用，保证两条路径的保留清理语义一致。
 */
@Component
@RequiredArgsConstructor
public class ProjectAttachmentHandler {

    private final ProjectAttachmentMapper projectAttachmentMapper;
    private final ResourceService resourceService;
    private final Clock clock;

    /** 逻辑删除单个附件关联，并把对应文件送入保留期。 */
    public void abandonAttachment(ProjectAttachment attachment, Long orgId) {
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = projectAttachmentMapper.update(null, new LambdaUpdateWrapper<ProjectAttachment>()
                .eq(ProjectAttachment::getId, attachment.getId())
                .set(ProjectAttachment::getDeletedAt, now)
                .set(ProjectAttachment::getUpdatedAt, now));
        if (updated != 1) {
            throw new AppException(ResultCode.OPERATION_FAIL, "附件已删除，不可重复操作");
        }
        abandonResources(List.of(attachment.getFileId()), orgId);
    }

    /** 逻辑删除企划下全部附件关联，并把对应文件送入保留期。 */
    public void abandonProjectAttachments(Long projectId, Long orgId) {
        List<ProjectAttachment> attachments = projectAttachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectAttachment>().eq(ProjectAttachment::getProjectId, projectId));
        if (attachments.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        projectAttachmentMapper.update(null, new LambdaUpdateWrapper<ProjectAttachment>()
                .eq(ProjectAttachment::getProjectId, projectId)
                .set(ProjectAttachment::getDeletedAt, now)
                .set(ProjectAttachment::getUpdatedAt, now));
        abandonResources(attachments.stream().map(ProjectAttachment::getFileId).distinct().toList(), orgId);
    }

    private void abandonResources(List<Long> fileIds, Long orgId) {
        resourceService.abandonBound(ResourceAbandonParamsBO.builder()
                .orgId(orgId)
                .resourceIds(fileIds)
                .build());
    }
}
