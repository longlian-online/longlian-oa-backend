package online.longlian.app.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.common.util.OpenQuerySupport;
import online.longlian.app.pojo.bo.app.project.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.app.project.ProjectAttachmentResultBO;
import online.longlian.app.pojo.bo.app.project.ProjectDetailResultBO;
import online.longlian.app.pojo.bo.app.project.ProjectTypeInfoResultBO;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenProjectItemBO;
import online.longlian.app.pojo.bo.open.OpenProjectItemListParamsBO;
import online.longlian.app.pojo.bo.open.OpenProjectMemberBO;
import online.longlian.app.pojo.bo.open.OpenProjectMemberListParamsBO;
import online.longlian.app.pojo.bo.open.OpenCursorBO;
import online.longlian.app.pojo.dto.open.OpenProjectItemListDTO;
import online.longlian.app.pojo.dto.open.OpenProjectMemberListDTO;
import online.longlian.app.pojo.vo.open.OpenPageVO;
import online.longlian.app.pojo.vo.open.OpenProjectAttachmentVO;
import online.longlian.app.pojo.vo.open.OpenProjectDetailVO;
import online.longlian.app.pojo.vo.open.OpenProjectItemVO;
import online.longlian.app.pojo.vo.open.OpenProjectMemberVO;
import online.longlian.app.pojo.vo.open.OpenProjectTypeVO;
import online.longlian.app.pojo.vo.open.OpenReadUrlVO;
import online.longlian.app.service.app.project.ProjectAttachmentService;
import online.longlian.app.service.app.project.ProjectService;
import online.longlian.app.pojo.bo.app.project.ProjectAttachmentDownloadParamsBO;
import online.longlian.app.pojo.bo.app.project.ProjectAttachmentListParamsBO;
import online.longlian.app.service.open.OpenProjectItemService;
import online.longlian.app.service.open.OpenProjectMemberService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放面企划只读查询。组织上下文全部来自密钥解出的 principal，不接受请求指定。
 */
@RequiredArgsConstructor
@Tag(name = "开放接口-企划查询", description = "密钥认证的企划详情、成员、类型、项目、附件")
@SecurityRequirement(name = "XApiKey")
@RequestMapping("/open/v1/projects")
@RestController
public class OpenProjectReadController {

    private final ProjectService projectService;
    private final ProjectAttachmentService projectAttachmentService;
    private final OpenProjectMemberService openProjectMemberService;
    private final OpenProjectItemService openProjectItemService;

    @Operation(summary = "企划详情", description = "不含管理视角字段与进度统计")
    @GetMapping("/{projectId}")
    public OpenProjectDetailVO getProjectDetail(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @PathVariable Long projectId) {
        ProjectDetailResultBO bo = projectService.getProjectDetail(projectId, principal.userId(), principal.orgId());
        OpenProjectDetailVO vo = new OpenProjectDetailVO();
        vo.setId(bo.getId());
        vo.setTitle(bo.getTitle());
        vo.setAlias(bo.getAlias());
        vo.setCoverUrl(bo.getCoverUrl());
        vo.setThumbnailUrl(bo.getThumbnailUrl());
        vo.setTypeId(bo.getTypeId());
        vo.setStatus(bo.getStatus());
        vo.setDescription(bo.getDescription());
        vo.setCreator(Boolean.TRUE.equals(bo.getIsCreator()));
        vo.setCreatedAt(bo.getCreatedAt());
        vo.setUpdatedAt(bo.getUpdatedAt());
        return vo;
    }

    @Operation(summary = "企划成员列表", description = "userId 键集游标，顺序翻页；无 updatedSince")
    @GetMapping("/{projectId}/members")
    public OpenPageVO<OpenProjectMemberVO> listProjectMembers(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @PathVariable Long projectId,
            @Valid OpenProjectMemberListDTO dto) {
        OpenPageResultBO<OpenProjectMemberBO> page = openProjectMemberService.listProjectMembers(
                new OpenProjectMemberListParamsBO(projectId, principal.orgId(),
                        OpenQuerySupport.parseCursor(dto.getCursor()),
                        OpenQuerySupport.resolveLimit(dto.getLimit())));
        List<OpenProjectMemberVO> items = page.list().stream().map(bo -> {
            OpenProjectMemberVO vo = new OpenProjectMemberVO();
            vo.setUserId(bo.userId());
            vo.setNickname(bo.nickname());
            vo.setJoinedAt(bo.joinedAt());
            vo.setCreator(bo.creator());
            vo.setCompletedCount(bo.completedCount());
            vo.setLastSubmittedAt(bo.lastSubmittedAt());
            return vo;
        }).toList();
        return new OpenPageVO<>(items, page.nextCursor(), page.hasMore());
    }

    @Operation(summary = "启用中的企划类型列表")
    @GetMapping("/types")
    public List<OpenProjectTypeVO> listProjectTypes(
            @AuthenticationPrincipal OpenApiPrincipal principal) {
        return projectService.getProjectTypes(principal.orgId()).stream()
                .map(type -> new OpenProjectTypeVO(type.getId(), type.getName()))
                .toList();
    }

    @Operation(summary = "企划下项目列表", description = "cursor + updatedSince；不含节点结构，节点走任务流接口")
    @GetMapping("/{projectId}/items")
    public OpenPageVO<OpenProjectItemVO> listProjectItems(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @PathVariable Long projectId,
            @Valid OpenProjectItemListDTO dto) {
        OpenPageResultBO<OpenProjectItemBO> page = openProjectItemService.listProjectItems(
                new OpenProjectItemListParamsBO(projectId, principal.orgId(),
                        OpenQuerySupport.parseCursor(dto.getCursor()),
                        OpenQuerySupport.parseUpdatedSince(dto.getUpdatedSince()),
                        OpenQuerySupport.resolveLimit(dto.getLimit())));
        List<OpenProjectItemVO> items = page.list().stream().map(bo -> {
            OpenProjectItemVO vo = new OpenProjectItemVO();
            vo.setId(bo.id());
            vo.setTitle(bo.title());
            vo.setStatus(bo.status() == null ? null : bo.status().getDesc());
            vo.setCurrentNodeName(bo.currentNodeName());
            vo.setProgressPercent(bo.progressPercent());
            vo.setHasSubmissionRecord(bo.hasSubmissionRecord());
            vo.setCreatedAt(bo.createdAt());
            vo.setUpdatedAt(bo.updatedAt());
            return vo;
        }).toList();
        return new OpenPageVO<>(items, page.nextCursor(), page.hasMore());
    }

    @Operation(summary = "企划附件列表", description = "仅元信息，读取链接按次签发")
    @GetMapping("/{projectId}/attachments")
    public List<OpenProjectAttachmentVO> listProjectAttachments(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @PathVariable Long projectId) {
        return projectAttachmentService.getProjectAttachments(ProjectAttachmentListParamsBO.builder()
                        .projectId(projectId)
                        .orgId(principal.orgId())
                        .build())
                .stream()
                .map(this::toAttachmentVO)
                .toList();
    }

    @Operation(summary = "企划附件读取链接", description = "按次签发，返回读取地址与到期时间")
    @GetMapping("/{projectId}/attachments/{attachmentId}/read-url")
    public OpenReadUrlVO getProjectAttachmentReadUrl(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @PathVariable Long projectId,
            @Parameter(description = "附件ID") @PathVariable Long attachmentId) {
        ProjectAttachmentDownloadResultBO bo = projectAttachmentService.getProjectAttachmentDownload(
                ProjectAttachmentDownloadParamsBO.builder()
                        .projectId(projectId)
                        .attachmentId(attachmentId)
                        .orgId(principal.orgId())
                        .build());
        return new OpenReadUrlVO(bo.getReadUrl(), bo.getExpireAt());
    }

    private OpenProjectAttachmentVO toAttachmentVO(ProjectAttachmentResultBO bo) {
        OpenProjectAttachmentVO vo = new OpenProjectAttachmentVO();
        vo.setFileId(bo.getFileId());
        vo.setName(bo.getFileName());
        vo.setSize(bo.getFileSize());
        vo.setMime(bo.getFileMime());
        vo.setUploaderId(bo.getUploaderId());
        vo.setUploaderName(bo.getUploaderName());
        vo.setCreatedAt(bo.getCreatedAt());
        return vo;
    }
}
