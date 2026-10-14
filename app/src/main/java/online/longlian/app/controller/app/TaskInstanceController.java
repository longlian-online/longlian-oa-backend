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
import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.bo.app.MyTaskParamsBO;
import online.longlian.app.pojo.bo.app.TaskPlazaParamsBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.dto.app.MyTaskListDTO;
import online.longlian.app.pojo.dto.app.TaskSubmitDTO;

import online.longlian.app.pojo.dto.app.TaskPlazaListDTO;
import online.longlian.app.pojo.dto.app.TaskRejectDTO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.bo.app.TaskSubmissionVersionsParamsBO;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.MyTaskVO;
import online.longlian.app.pojo.vo.app.PlazaProjectGroupVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.app.MyTaskService;
import online.longlian.app.service.app.TaskInstanceService;
import online.longlian.app.service.app.TaskPlazaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "任务实例接口", description = "任务接取、放弃、提交、重置、打回、查看详情、按项目查询实例列表")
@RequestMapping("/app/task/instance")
@RestController
@RequiredArgsConstructor
public class TaskInstanceController {

    private final TaskInstanceService taskInstanceService;
    private final TaskPlazaService taskPlazaService;
    private final MyTaskService myTaskService;

    @Operation(
            summary = "我的待办",
            description = "分页返回当前用户在当前组织内已接取（CLAIMED）的任务。" +
                    "bucket=SUBMITTABLE 前置阶段全部完成，WAITING 存在未完成前置；默认按接取时间升序"
    )
    @GetMapping("/mine")
    @ResponseMessage("查询成功")
    public PageResultVO<MyTaskVO> listMyTasks(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @ModelAttribute @Valid MyTaskListDTO listDTO) {
        return myTaskService.listMyTasks(
                MyTaskParamsBO.builder()
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .bucket(listDTO.getBucket())
                        .sortDirection(listDTO.getSortDirection())
                        .page(new PageParamsBO(listDTO.getPageNum(), listDTO.getPageSize()))
                        .build());
    }

    @Operation(
            summary = "任务广场（缺人任务）",
            description = "分页返回当前组织内可接取的缺人任务，按企划分组。" +
                    "stageScope=ALL 返回所有阶段，CURRENT（默认）仅返回首个未完成阶段"
    )
    @GetMapping("/plaza")
    @ResponseMessage("查询成功")
    public PageResultVO<PlazaProjectGroupVO> getTaskPlaza(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @ModelAttribute @Valid TaskPlazaListDTO listDTO) {
        return taskPlazaService.getTaskPlaza(
                TaskPlazaParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .keyword(listDTO.getKeyword())
                        .projectType(listDTO.getProjectType())
                        .sortBy(listDTO.getSortBy())
                        .stageScope(listDTO.getStageScope())
                        .page(new PageParamsBO(listDTO.getPageNum(), listDTO.getPageSize()))
                        .build());
    }

    @Operation(
            summary = "查询项目下的任务实例列表",
            description = "返回当前项目所有已生成的任务实例"
    )
    @Parameter(name = "itemId", description = "项目ID")
    @GetMapping("/item/{itemId}")
    @ResponseMessage("查询成功")
    public List<ItemTaskInstanceVO> listItemTaskInstances(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                                           @PathVariable Long itemId) {
        return taskInstanceService.listItemTaskInstances(
                TaskInstanceListParamsBO.builder()
                        .itemId(itemId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .build());
    }

    @Operation(
            summary = "查看任务实例详情",
            description = "返回任务信息及服务器组装的提交字段和附件"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @GetMapping("/{instanceId}/detail")
    @ResponseMessage("查询成功")
    public TaskInstanceDetailVO getTaskInstanceDetail(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                                       @PathVariable Long instanceId) {
        return taskInstanceService.getTaskInstanceDetail(
                TaskInstanceDetailParamsBO.builder()
                        .instanceId(instanceId)
                        .orgId(sessionContext.orgId())
                        .build());
    }

    @Operation(
            summary = "任务提交版本列表",
            description = "按版本号倒序返回该任务实例的全部提交版本，包含提交人/打回人与按表单快照组织的字段和附件"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @GetMapping("/{instanceId}/submissions")
    @ResponseMessage("查询成功")
    public List<TaskSubmissionVersionVO> listSubmissionVersions(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long instanceId) {
        return taskInstanceService.listSubmissionVersions(
                TaskSubmissionVersionsParamsBO.builder()
                        .instanceId(instanceId)
                        .orgId(sessionContext.orgId())
                        .build());
    }

    @Operation(
        summary = "接取任务",
        description = "接取 PENDING 状态的任务，接取后任务状态变为 CLAIMED，归属当前登录用户"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/claim")
    @ResponseMessage("接取成功")
    public void claimTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                           @PathVariable Long instanceId) {
        taskInstanceService.claimTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .build());
    }

    @Operation(
        summary = "放弃任务",
        description = "放弃已接取（CLAIMED）的任务，任务状态恢复为 PENDING，可由他人重新接取"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/abandon")
    @ResponseMessage("已放弃")
    public void abandonTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                             @PathVariable Long instanceId) {
        taskInstanceService.abandonTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .build());
    }

    @Operation(
        summary = "提交任务",
        description = "提交待提交（CLAIMED）的任务，提交后任务状态变为 COMPLETED。提交内容通过 values 传递"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/submit")
    @ResponseMessage("提交成功")
    public void submitTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                            @PathVariable Long instanceId,
                            @RequestBody @Valid TaskSubmitDTO taskSubmitDTO) {
        taskInstanceService.submitTask(
                TaskInstanceSubmitParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .values(taskSubmitDTO.getValues())
                        .build());
    }

    @Operation(
        summary = "重置任务提交",
        description = "将已完成（COMPLETED）的任务重置为 CLAIMED 状态，允许重新提交。"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/reset")
    @ResponseMessage("已重置")
    public void resetTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                           @PathVariable Long instanceId) {
        taskInstanceService.resetTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .build());
    }

    @Operation(
        summary = "打回任务",
        description = "下一阶段执行人可将已完成（COMPLETED）的任务打回，" +
                "任务恢复为 CLAIMED 状态，提交记录标记为 REJECTED，需填写打回意见"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/reject")
    @ResponseMessage("已打回")
    public void rejectTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                            @PathVariable Long instanceId,
                            @RequestBody @Valid TaskRejectDTO taskRejectDTO) {
        taskInstanceService.rejectTask(
                TaskInstanceRejectParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .reviewComment(taskRejectDTO.getReviewComment())
                        .build());
    }

}
