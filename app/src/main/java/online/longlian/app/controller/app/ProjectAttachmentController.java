package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.ProjectAttachmentCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDeleteParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentListParamsBO;
import online.longlian.app.pojo.dto.app.ProjectAttachmentCreateDTO;
import online.longlian.app.pojo.vo.app.ProjectAttachmentDownloadVO;
import online.longlian.app.pojo.vo.app.ProjectAttachmentVO;
import online.longlian.app.service.app.ProjectAttachmentService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "企划附件接口", description = "企划详情附件区域")
@RequestMapping("/app/projects")
@RequiredArgsConstructor
@RestController
public class ProjectAttachmentController {

    private final ProjectAttachmentService projectAttachmentService;

    @Operation(
            summary = "获取企划附件列表",
            description = "按上传时间新到旧返回；仅组织内可见企划可访问。列表只输出已授权文件元信息，不签发读取链接"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @GetMapping("/{projectId}/attachments")
    @ResponseMessage("查询成功")
    public List<ProjectAttachmentVO> getProjectAttachments(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long projectId) {
        return projectAttachmentService.getProjectAttachments(ProjectAttachmentListParamsBO.builder()
                        .projectId(projectId)
                        .orgId(sessionContext.orgId())
                        .build()).stream()
                .map(resultBO -> {
                    ProjectAttachmentVO vo = new ProjectAttachmentVO();
                    BeanUtils.copyProperties(resultBO, vo);
                    return vo;
                })
                .toList();
    }

    @Operation(
            summary = "绑定企划附件",
            description = "仅企划负责人可操作；文件须为当前上传人在本组织未绑定的 project_attachment 上传"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @PostMapping("/{projectId}/attachments")
    @ResponseMessage("添加成功")
    public void createProjectAttachment(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectAttachmentCreateDTO projectAttachmentCreateDTO) {
        projectAttachmentService.createProjectAttachment(ProjectAttachmentCreateParamsBO.builder()
                .projectId(projectId)
                .fileId(projectAttachmentCreateDTO.getFileId())
                .userId(sessionContext.userId())
                .orgId(sessionContext.orgId())
                .build());
    }

    @Operation(summary = "删除企划附件", description = "仅企划负责人可操作；逻辑删除关联并把文件送入资源保留期")
    @Parameter(name = "projectId", description = "企划ID")
    @Parameter(name = "attachmentId", description = "附件ID")
    @DeleteMapping("/{projectId}/attachments/{attachmentId}")
    @ResponseMessage("删除成功")
    public void deleteProjectAttachment(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long projectId,
            @PathVariable Long attachmentId) {
        projectAttachmentService.deleteProjectAttachment(ProjectAttachmentDeleteParamsBO.builder()
                .projectId(projectId)
                .attachmentId(attachmentId)
                .userId(sessionContext.userId())
                .orgId(sessionContext.orgId())
                .build());
    }

    @Operation(summary = "获取企划附件下载链接", description = "点击下载时新取签名，返回读取地址和到期时间（Unix 秒）")
    @Parameter(name = "projectId", description = "企划ID")
    @Parameter(name = "attachmentId", description = "附件ID")
    @GetMapping("/{projectId}/attachments/{attachmentId}/download")
    @ResponseMessage("获取成功")
    public ProjectAttachmentDownloadVO getProjectAttachmentDownload(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long projectId,
            @PathVariable Long attachmentId) {
        ProjectAttachmentDownloadResultBO resultBO = projectAttachmentService.getProjectAttachmentDownload(
                ProjectAttachmentDownloadParamsBO.builder()
                        .projectId(projectId)
                        .attachmentId(attachmentId)
                        .orgId(sessionContext.orgId())
                        .build());
        ProjectAttachmentDownloadVO vo = new ProjectAttachmentDownloadVO();
        BeanUtils.copyProperties(resultBO, vo);
        return vo;
    }
}
