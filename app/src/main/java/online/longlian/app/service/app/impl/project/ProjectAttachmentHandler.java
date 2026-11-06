package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectAttachmentMapper;
import online.longlian.app.pojo.entity.ProjectAttachment;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 企划附件关联的删除处理：只逻辑删除关联。
 * <p>
 * 文件是否仍被引用由资源弃置任务判断，业务删除不再改资源状态。
 * 供单附件删除与企划整体删除共用。
 */
@Component
@RequiredArgsConstructor
public class ProjectAttachmentHandler {

    private final ProjectAttachmentMapper projectAttachmentMapper;
    private final Clock clock;

    /** 逻辑删除单个附件关联。 */
    public void deleteAttachment(ProjectAttachment attachment) {
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = projectAttachmentMapper.update(null, new LambdaUpdateWrapper<ProjectAttachment>()
                .eq(ProjectAttachment::getId, attachment.getId())
                .set(ProjectAttachment::getDeletedAt, now)
                .set(ProjectAttachment::getUpdatedAt, now));
        if (updated != 1) {
            throw new AppException(ResultCode.OPERATION_FAIL, "附件已删除，不可重复操作");
        }
    }

    /** 逻辑删除企划下全部附件关联。 */
    public void deleteProjectAttachments(Long projectId) {
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
    }
}
