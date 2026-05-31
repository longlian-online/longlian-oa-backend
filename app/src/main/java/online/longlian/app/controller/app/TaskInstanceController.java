package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.dto.app.TaskRejectDTO;
import online.longlian.app.pojo.dto.app.TaskSubmitDTO;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.service.app.TaskInstanceService;
import online.longlian.app.service.common.CurrentOrganizationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "任务实例接口", description = "任务接取、放弃、提交、重置、打回、查看详情、按项目查询实例列表")
@RequestMapping("/app/task/instance")
@RestController
@RequiredArgsConstructor
public class TaskInstanceController {

    private final TaskInstanceService taskInstanceService;

    @Operation(
            summary = "查询项目下的任务实例列表",
            description = "返回当前项目所有已生成的任务实例"
    )
    @Parameter(name = "itemId", description = "项目ID")
    @GetMapping("/item/{itemId}")
    @ResponseMessage("查询成功")
    public List<ItemTaskInstanceVO> listItemTaskInstances(@UserSession SessionContext sessionContext,
                                                           @PathVariable Long itemId) {
        return taskInstanceService.listItemTaskInstances(
                TaskInstanceListParamsBO.builder()
                        .itemId(itemId)
                        .userId(userId)
                        .orgId(orgId)
                        .build());
    }

    @Operation(
            summary = "查看任务实例详情",
            description = "返回任务实例信息及最近一次提交的元数据详情"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @GetMapping("/{instanceId}/detail")
    @ResponseMessage("查询成功")
    public TaskInstanceDetailVO getTaskInstanceDetail(@UserSession SessionContext sessionContext,
                                                       @PathVariable Long instanceId) {
        return taskInstanceService.getTaskInstanceDetail(
                TaskInstanceDetailParamsBO.builder()
                        .instanceId(instanceId)
                        .build());
    }

    @Operation(
        summary = "接取任务",
        description = "接取 PENDING 状态的任务，接取后任务状态变为 CLAIMED，归属当前登录用户"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/claim")
    @ResponseMessage("接取成功")
    public void claimTask(@UserSession SessionContext sessionContext,
                           @PathVariable Long instanceId) {
        taskInstanceService.claimTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .build());
    }

    @Operation(
        summary = "放弃任务",
        description = "放弃已接取（CLAIMED）的任务，任务状态恢复为 PENDING，可由他人重新接取"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/abandon")
    @ResponseMessage("已放弃")
    public void abandonTask(@UserSession SessionContext sessionContext,
                             @PathVariable Long instanceId) {
        taskInstanceService.abandonTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .build());
    }

    @Operation(
        summary = "提交任务",
        description = "提交待提交（CLAIMED）的任务，提交后任务状态变为 COMPLETED。提交内容通过 metadata(JSON对象) 传递"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/submit")
    @ResponseMessage("提交成功")
    public void submitTask(@UserSession SessionContext sessionContext,
                            @PathVariable Long instanceId,
                            @RequestBody @Valid TaskSubmitDTO taskSubmitDTO) {
        taskInstanceService.submitTask(
                TaskInstanceSubmitParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .metadata(taskSubmitDTO.getMetadata())
                        .build());
    }

    @Operation(
        summary = "重置任务提交",
        description = "将已完成（COMPLETED）的任务重置为 CLAIMED 状态，允许重新提交。"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/reset")
    @ResponseMessage("已重置")
    public void resetTask(@UserSession SessionContext sessionContext,
                           @PathVariable Long instanceId) {
        taskInstanceService.resetTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
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
    public void rejectTask(@UserSession SessionContext sessionContext,
                            @PathVariable Long instanceId,
                            @RequestBody @Valid TaskRejectDTO taskRejectDTO) {
        taskInstanceService.rejectTask(
                TaskInstanceRejectParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .reviewComment(taskRejectDTO.getReviewComment())
                        .build());
    }

}
