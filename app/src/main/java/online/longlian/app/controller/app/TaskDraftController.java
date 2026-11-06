package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.TaskDraftContentResultBO;
import online.longlian.app.pojo.bo.app.TaskDraftGetParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftResultBO;
import online.longlian.app.pojo.bo.app.TaskDraftSaveParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSubmitParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSubmitResultBO;
import online.longlian.app.pojo.dto.app.TaskDraftSaveDTO;
import online.longlian.app.pojo.dto.app.TaskDraftSubmitDTO;
import online.longlian.app.pojo.vo.app.TaskDraftContentVO;
import online.longlian.app.pojo.vo.app.TaskDraftDetailVO;
import online.longlian.app.pojo.vo.app.TaskDraftFileVO;
import online.longlian.app.pojo.vo.app.TaskDraftSubmitResultVO;
import online.longlian.app.pojo.vo.app.TaskDraftTaskVO;
import online.longlian.app.service.app.TaskInstanceService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "任务草稿接口", description = "接取人的任务成果暂存与正式提交")
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/task/instance/{instanceId}/draft")
public class TaskDraftController {

    private final TaskInstanceService taskInstanceService;

    @Operation(summary = "获取任务草稿")
    @Parameter(name = "instanceId", description = "任务实例ID")
    @GetMapping
    @ResponseMessage("查询成功")
    public TaskDraftDetailVO getTaskDraft(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long instanceId) {
        TaskDraftResultBO result = taskInstanceService.getTaskDraft(TaskDraftGetParamsBO.builder()
                .instanceId(instanceId)
                .userId(sessionContext.userId())
                .orgId(sessionContext.orgId())
                .build());
        return toDetailVO(result);
    }

    @Operation(summary = "暂存任务草稿", description = "允许暂存未完成的表单；任务和草稿修订号须与当前版本一致")
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PutMapping
    @ResponseMessage("暂存成功")
    public TaskDraftDetailVO saveTaskDraft(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long instanceId,
            @RequestBody @Valid TaskDraftSaveDTO taskDraftSaveDTO) {
        TaskDraftSaveParamsBO params = new TaskDraftSaveParamsBO();
        BeanUtils.copyProperties(taskDraftSaveDTO, params);
        params.setInstanceId(instanceId);
        params.setUserId(sessionContext.userId());
        params.setOrgId(sessionContext.orgId());
        return toDetailVO(taskInstanceService.saveTaskDraft(params));
    }

    @Operation(summary = "正式提交任务草稿", description = "只提交指定修订号的已暂存内容，同一版本重复提交返回原结果")
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/submit")
    @ResponseMessage("提交成功")
    public TaskDraftSubmitResultVO submitTaskDraft(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long instanceId,
            @RequestBody @Valid TaskDraftSubmitDTO taskDraftSubmitDTO) {
        TaskDraftSubmitParamsBO params = new TaskDraftSubmitParamsBO();
        BeanUtils.copyProperties(taskDraftSubmitDTO, params);
        params.setInstanceId(instanceId);
        params.setUserId(sessionContext.userId());
        params.setOrgId(sessionContext.orgId());
        TaskDraftSubmitResultBO result = taskInstanceService.submitTaskDraft(params);
        TaskDraftSubmitResultVO vo = new TaskDraftSubmitResultVO();
        BeanUtils.copyProperties(result, vo);
        return vo;
    }

    private TaskDraftDetailVO toDetailVO(TaskDraftResultBO result) {
        TaskDraftDetailVO vo = new TaskDraftDetailVO();
        BeanUtils.copyProperties(result, vo);
        vo.setTask(new TaskDraftTaskVO(result.getInstanceId(), result.getItemId(),
                result.getTaskRevision(), "CLAIMED"));
        if (result.getDraft() != null) {
            vo.setDraft(toContentVO(result.getDraft()));
        }
        return vo;
    }

    private TaskDraftContentVO toContentVO(TaskDraftContentResultBO result) {
        TaskDraftContentVO vo = new TaskDraftContentVO();
        BeanUtils.copyProperties(result, vo);
        Map<String, TaskDraftFileVO> files = new LinkedHashMap<>();
        result.getFiles().forEach((fileId, file) -> {
            TaskDraftFileVO fileVO = new TaskDraftFileVO();
            BeanUtils.copyProperties(file, fileVO);
            files.put(fileId, fileVO);
        });
        vo.setFiles(files);
        return vo;
    }
}
