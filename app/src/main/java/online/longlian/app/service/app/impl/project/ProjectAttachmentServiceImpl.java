package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectAttachmentMapper;
import online.longlian.app.pojo.bo.app.ProjectAccessParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDeleteParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentResultBO;
import online.longlian.app.pojo.bo.common.ActivatedResource;
import online.longlian.app.pojo.bo.common.ActivatedResourceRead;
import online.longlian.app.pojo.bo.common.ResourceBindParamsBO;
import online.longlian.app.pojo.entity.ProjectAttachment;
import online.longlian.app.service.app.ProjectAttachmentService;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.app.UserService;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectAttachmentServiceImpl implements ProjectAttachmentService {

    /** 企划附件的资源业务类型；文件元信息仍由 resource 表承载。 */
    private static final String BIZ_TYPE = "project_attachment";

    private final ProjectAttachmentMapper projectAttachmentMapper;
    private final ProjectService projectService;
    private final ResourceService resourceService;
    private final UserService userService;
    private final ProjectAttachmentHandler projectAttachmentHandler;
    private final Clock clock;

    @Override
    public List<ProjectAttachmentResultBO> getProjectAttachments(ProjectAttachmentListParamsBO params) {
        projectService.requireVisibleProject(new ProjectAccessParamsBO(params.getProjectId(), params.getOrgId()));
        List<ProjectAttachment> attachments = projectAttachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectAttachment>()
                        .eq(ProjectAttachment::getProjectId, params.getProjectId())
                        .orderByDesc(ProjectAttachment::getCreatedAt)
                        .orderByDesc(ProjectAttachment::getId));
        if (attachments.isEmpty()) {
            return List.of();
        }
        List<Long> fileIds = attachments.stream().map(ProjectAttachment::getFileId).distinct().toList();
        Map<Long, ActivatedResource> resources = resourceService.findActivated(
                BIZ_TYPE, params.getProjectId(), params.getOrgId(), fileIds);
        Map<Long, String> uploaderNames = userService.displayNames(
                attachments.stream().map(ProjectAttachment::getUploaderId).distinct().toList());
        return attachments.stream()
                .filter(attachment -> resources.containsKey(attachment.getFileId()))
                .map(attachment -> {
                    ActivatedResource resource = resources.get(attachment.getFileId());
                    return ProjectAttachmentResultBO.builder()
                            .id(attachment.getId())
                            .fileId(attachment.getFileId())
                            .fileName(resource.fileName())
                            .fileSize(resource.fileSize())
                            .fileMime(resource.fileMime())
                            .uploaderId(attachment.getUploaderId())
                            .uploaderName(uploaderNames.get(attachment.getUploaderId()))
                            .createdAt(attachment.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProjectAttachment(ProjectAttachmentCreateParamsBO params) {
        requireProjectCreator(params.getProjectId(), params.getUserId(), params.getOrgId());
        resourceService.bindBizResource(ResourceBindParamsBO.builder()
                .resourceId(params.getFileId())
                .bizType(BIZ_TYPE)
                .bizId(params.getProjectId())
                .creatorId(params.getUserId())
                .orgId(params.getOrgId())
                .build());
        LocalDateTime now = LocalDateTime.now(clock);
        projectAttachmentMapper.insert(ProjectAttachment.builder()
                .projectId(params.getProjectId())
                .fileId(params.getFileId())
                .uploaderId(params.getUserId())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectAttachment(ProjectAttachmentDeleteParamsBO params) {
        requireProjectCreator(params.getProjectId(), params.getUserId(), params.getOrgId());
        ProjectAttachment attachment = requireAttachment(params.getProjectId(), params.getAttachmentId());
        projectAttachmentHandler.abandonAttachment(attachment, params.getOrgId());
    }

    @Override
    public ProjectAttachmentDownloadResultBO getProjectAttachmentDownload(ProjectAttachmentDownloadParamsBO params) {
        projectService.requireVisibleProject(new ProjectAccessParamsBO(params.getProjectId(), params.getOrgId()));
        ProjectAttachment attachment = requireAttachment(params.getProjectId(), params.getAttachmentId());
        Map<Long, ActivatedResource> matched = resourceService.findActivated(
                BIZ_TYPE, params.getProjectId(), params.getOrgId(), List.of(attachment.getFileId()));
        ActivatedResource resource = matched.get(attachment.getFileId());
        if (resource == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "附件不存在");
        }
        ActivatedResourceRead read = resourceService.signDownload(resource);
        return ProjectAttachmentDownloadResultBO.builder()
                .readUrl(read.readUrl())
                .expireAt(read.expiresAt())
                .build();
    }

    private ProjectAttachment requireAttachment(Long projectId, Long attachmentId) {
        ProjectAttachment attachment = projectAttachmentMapper.selectOne(new LambdaQueryWrapper<ProjectAttachment>()
                .eq(ProjectAttachment::getId, attachmentId)
                .eq(ProjectAttachment::getProjectId, projectId));
        if (attachment == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "附件不存在");
        }
        return attachment;
    }

    /** 附件属于企划编辑范围；加锁保证与企划删除互斥。创建者和本组织管理者可增删。 */
    private void requireProjectCreator(Long projectId, Long userId, Long orgId) {
        Long creatorId = projectService.lockEnabledProject(new ProjectAccessParamsBO(projectId, orgId)).getCreatorId();
        if (!creatorId.equals(userId) && !projectService.isPlanningManager(creatorId, userId, orgId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅企划负责人可编辑附件");
        }
    }
}
