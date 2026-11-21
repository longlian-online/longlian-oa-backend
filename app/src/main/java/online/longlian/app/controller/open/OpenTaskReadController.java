package online.longlian.app.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.pojo.bo.app.project.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskAttachmentDownloadParamsBO;
import online.longlian.app.pojo.bo.open.OpenSubmissionFieldBO;
import online.longlian.app.pojo.bo.open.OpenSubmissionVersionBO;
import online.longlian.app.pojo.bo.open.OpenTaskDetailBO;
import online.longlian.app.pojo.bo.open.OpenTaskDetailParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmissionsParamsBO;
import online.longlian.app.pojo.vo.open.OpenReadUrlVO;
import online.longlian.app.pojo.vo.open.OpenSubmissionVersionVO;
import online.longlian.app.pojo.vo.open.OpenTaskDetailVO;
import online.longlian.app.service.app.taskinstance.TaskInstanceService;
import online.longlian.app.service.open.OpenTaskQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放面任务实例只读查询。组织上下文全部来自密钥解出的 principal，不接受请求指定。
 */
@RequiredArgsConstructor
@Tag(name = "开放接口-任务实例查询", description = "密钥认证的任务详情、提交版本、附件读取链接")
@SecurityRequirement(name = "XApiKey")
@RequestMapping("/open/v1/tasks")
@RestController
public class OpenTaskReadController {

    private final TaskInstanceService taskInstanceService;
    private final OpenTaskQueryService openTaskQueryService;

    @Operation(summary = "任务实例详情",
            description = "含当前提交的字段与附件名（不含读取链接）；versionNo 与附件读取链接另行获取")
    @GetMapping("/{instanceId}/detail")
    public OpenTaskDetailVO getTaskDetail(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @PathVariable Long instanceId) {
        OpenTaskDetailBO bo = openTaskQueryService.getTaskDetail(
                new OpenTaskDetailParamsBO(instanceId, principal.orgId()));
        return toDetailVO(bo);
    }

    @Operation(summary = "任务提交版本列表", description = "版本号倒序全量返回；附件只含名称与大小")
    @GetMapping("/{instanceId}/submissions")
    public List<OpenSubmissionVersionVO> listSubmissions(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @PathVariable Long instanceId) {
        return openTaskQueryService.listSubmissions(
                        new OpenTaskSubmissionsParamsBO(instanceId, principal.orgId()))
                .stream()
                .map(this::toVersionVO)
                .toList();
    }

    @Operation(summary = "任务附件读取链接", description = "按次签发，返回读取地址与到期时间")
    @GetMapping("/{instanceId}/attachments/{fileId}/read-url")
    public OpenReadUrlVO getTaskAttachmentReadUrl(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @PathVariable Long instanceId,
            @Parameter(description = "文件ID") @PathVariable Long fileId) {
        ProjectAttachmentDownloadResultBO bo = taskInstanceService.getTaskAttachmentDownload(
                TaskAttachmentDownloadParamsBO.builder()
                        .instanceId(instanceId)
                        .fileId(fileId)
                        .orgId(principal.orgId())
                        .build());
        return new OpenReadUrlVO(bo.getReadUrl(), bo.getExpireAt());
    }

    private OpenTaskDetailVO toDetailVO(OpenTaskDetailBO bo) {
        OpenTaskDetailVO vo = new OpenTaskDetailVO();
        vo.setTaskInstanceId(bo.instanceId());
        vo.setName(bo.name());
        vo.setStage(bo.stage());
        vo.setSlotIndex(bo.slotIndex());
        vo.setSlotTotal(bo.slotTotal());
        vo.setSerialStep(bo.serialStep());
        vo.setItemId(bo.itemId());
        vo.setItemTitle(bo.itemTitle());
        vo.setProjectId(bo.projectId());
        vo.setProjectTitle(bo.projectTitle());
        vo.setStatus(bo.status());
        vo.setRevision(bo.revision());
        if (bo.assignee() != null) {
            vo.setAssignee(new OpenTaskDetailVO.TaskAssigneeVO(bo.assignee().userId(), bo.assignee().nickname()));
        }
        OpenTaskDetailBO.SubmissionBO submission = bo.submission();
        vo.setSubmission(new OpenTaskDetailVO.TaskSubmissionVO(
                submission.state(), submission.submittedAt(), toFieldVOs(submission.values())));
        vo.setSubmissionCount(bo.submissionCount());
        return vo;
    }

    private OpenSubmissionVersionVO toVersionVO(OpenSubmissionVersionBO bo) {
        OpenSubmissionVersionVO vo = new OpenSubmissionVersionVO();
        vo.setVersionNo(bo.versionNo());
        vo.setSubmittedAt(bo.submittedAt());
        if (bo.submitter() != null) {
            vo.setSubmitter(new OpenSubmissionVersionVO.SubmitterVO(bo.submitter().userId(), bo.submitter().nickname()));
        }
        vo.setValues(toFieldVOs(bo.values()));
        return vo;
    }

    private List<OpenTaskDetailVO.TaskFieldValueVO> toFieldVOs(List<OpenSubmissionFieldBO> fields) {
        if (fields == null) {
            return List.of();
        }
        return fields.stream()
                .map(field -> new OpenTaskDetailVO.TaskFieldValueVO(
                        field.key(), field.label(), field.type(), field.text(),
                        field.file() == null ? null : new OpenTaskDetailVO.TaskFileValueVO(
                                field.file().fileId(), field.file().name(), field.file().size())))
                .toList();
    }
}
