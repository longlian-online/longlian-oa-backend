package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileExpansionParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileExpansionResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileBatchFileResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileBatchTargetParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileDiscardParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileDownloadParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileEntriesResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileEntryResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileListParamsBO;
import online.longlian.app.pojo.dto.orgadmin.OrgFileExpansionDTO;
import online.longlian.app.pojo.dto.orgadmin.OrgFileBatchTargetDTO;
import online.longlian.app.pojo.vo.orgadmin.OrgFileExpansionVO;
import online.longlian.app.pojo.vo.orgadmin.OrgFileBatchFileVO;
import online.longlian.app.pojo.vo.orgadmin.OrgFileDownloadVO;
import online.longlian.app.pojo.vo.orgadmin.OrgFileEntriesVO;
import online.longlian.app.pojo.vo.orgadmin.OrgFileEntryVO;
import online.longlian.app.service.orgadmin.OrgFileService;
import online.longlian.common.enumeration.OrgFileView;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "组织文件", description = "组织管理端的企划、项目和任务提交文件")
@RequestMapping("/orgadmin/files")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class OrgFileController {

    private final OrgFileService orgFileService;

    @Operation(summary = "浏览组织文件", description = "不传 ID 为全部企划类型。传 typeId 为该类型下的企划。再传 projectId 后，view 省略或 item 为该企划下的项目，再传 itemId 为该项目全部版本的提交文件；view=task 为该企划下当前任务节点的并集，再传 taskId 为这些节点全部版本的提交文件。任务并集按节点上的原子任务标识归并，不读取原子任务表。类型、企划、任务和任务下的文件分页，项目下的提交文件不分页")
    @GetMapping
    @ResponseMessage("查询成功")
    public OrgFileEntriesVO list(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @Parameter(description = "打开企划后的视图，默认 item") @RequestParam(required = false) OrgFileView view,
            @Parameter(description = "企划类型 ID") @RequestParam(required = false) Long typeId,
            @Parameter(description = "企划 ID") @RequestParam(required = false) Long projectId,
            @Parameter(description = "项目 ID，仅项目视图") @RequestParam(required = false) Long itemId,
            @Parameter(description = "任务标识，仅任务视图。取自项目任务节点上的原子任务 ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "页码，从 1 开始") @RequestParam(required = false) Integer pageNum) {
        OrgFileEntriesResultBO result = orgFileService.list(OrgFileListParamsBO.builder()
                .orgId(sessionContext.orgId())
                .view(view)
                .typeId(typeId)
                .projectId(projectId)
                .itemId(itemId)
                .taskId(taskId)
                .pageNum(pageNum == null || pageNum < 1 ? 1 : pageNum)
                .pageSize(OrgFileService.PAGE_SIZE)
                .build());
        OrgFileEntriesVO vo = new OrgFileEntriesVO();
        BeanUtils.copyProperties(result, vo);
        vo.setEntries(toEntries(result.getEntries()));
        return vo;
    }

    @Operation(summary = "获取提交文件读取链接", description = "只签发该任务当前有效提交中的文件")
    @GetMapping("/tasks/{taskInstanceId}/attachments/{fileId}/download")
    @ResponseMessage("获取成功")
    public OrgFileDownloadVO download(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long taskInstanceId,
            @PathVariable Long fileId) {
        ProjectAttachmentDownloadResultBO result = orgFileService.download(OrgFileDownloadParamsBO.builder()
                .orgId(sessionContext.orgId())
                .taskInstanceId(taskInstanceId)
                .fileId(fileId)
                .build());
        OrgFileDownloadVO vo = new OrgFileDownloadVO();
        vo.setReadUrl(result.getReadUrl());
        vo.setExpireAt(result.getExpireAt());
        return vo;
    }

    @Operation(summary = "展开选中的提交文件", description = "展开选中的文件、项目、企划、企划类型或任务。只返回当前有效且可用的提交文件身份和原名，不签发读取链接")
    @PostMapping("/expansions")
    @ResponseMessage("获取成功")
    public OrgFileExpansionVO expand(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @RequestBody @Valid OrgFileExpansionDTO dto) {
        List<OrgFileBatchTargetParamsBO> targets = dto.getTargets().stream()
                .map(this::toTarget)
                .toList();
        OrgFileExpansionResultBO result = orgFileService.expand(OrgFileExpansionParamsBO.builder()
                .orgId(sessionContext.orgId())
                .targets(targets)
                .build());
        OrgFileExpansionVO vo = new OrgFileExpansionVO();
        vo.setFiles(result.getFiles().stream().map(this::toBatchFile).toList());
        return vo;
    }

    @Operation(summary = "弃置当前提交中的文件", description = "不改提交内容。封面、头像或企划附件仍引用时拒绝。弃置后文件页不再列出，任务详情显示为不可用")
    @DeleteMapping("/tasks/{taskInstanceId}/attachments/{fileId}")
    @ResponseMessage("已删除")
    public void discard(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long taskInstanceId,
            @PathVariable Long fileId,
            @Parameter(description = "提交字段 key") @RequestParam String fieldKey) {
        orgFileService.discard(OrgFileDiscardParamsBO.builder()
                .orgId(sessionContext.orgId())
                .taskInstanceId(taskInstanceId)
                .fileId(fileId)
                .fieldKey(fieldKey)
                .build());
    }

    private List<OrgFileEntryVO> toEntries(List<OrgFileEntryResultBO> entries) {
        if (entries == null) {
            return List.of();
        }
        return entries.stream().map(entry -> {
            OrgFileEntryVO vo = new OrgFileEntryVO();
            BeanUtils.copyProperties(entry, vo);
            return vo;
        }).toList();
    }

    private OrgFileBatchTargetParamsBO toTarget(OrgFileBatchTargetDTO target) {
        return OrgFileBatchTargetParamsBO.builder()
                .kind(target.getKind())
                .taskInstanceId(target.getTaskInstanceId())
                .fileId(target.getFileId())
                .fieldKey(target.getFieldKey())
                .itemId(target.getItemId())
                .projectId(target.getProjectId())
                .typeId(target.getTypeId())
                .taskId(target.getTaskId())
                .build();
    }

    private OrgFileBatchFileVO toBatchFile(OrgFileBatchFileResultBO file) {
        OrgFileBatchFileVO vo = new OrgFileBatchFileVO();
        BeanUtils.copyProperties(file, vo);
        return vo;
    }
}
