package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.dto.app.TaskRejectDTO;
import online.longlian.app.pojo.dto.app.TaskSubmitDTO;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.app.TaskInstanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "任务实例接口", description = "任务接取、放弃、提交、重置、打回、查看详情、按项目查询实例列表")
@RequestMapping("/app/task/instance")
@RestController
@RequiredArgsConstructor
public class TaskInstanceController {

    private final TaskInstanceService taskInstanceService;
    private final SessionService sessionService;

    @Operation(
            summary = "查询项目下的任务实例列表",
            description = "返回当前项目所有已生成的任务实例"
    )
    @Parameter(name = "itemId", description = "项目ID")
    @GetMapping("/item/{itemId}")
    public Result<List<ItemTaskInstanceVO>> listItemTaskInstances(@PathVariable Long itemId) {
        List<ItemTaskInstanceVO> instances = taskInstanceService.listItemTaskInstances(
                TaskInstanceListParamsBO.builder()
                        .itemId(itemId)
                        .build());
        return Result.success("查询成功", instances);
    }

    @Operation(
            summary = "查看任务实例详情",
            description = "返回任务实例信息及最近一次提交的元数据详情"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @GetMapping("/{instanceId}/detail")
    public Result<TaskInstanceDetailVO> getTaskInstanceDetail(@PathVariable Long instanceId) {
        TaskInstanceDetailVO taskInstanceDetailVO = taskInstanceService.getTaskInstanceDetail(
                TaskInstanceDetailParamsBO.builder()
                        .instanceId(instanceId)
                        .build());
        return Result.success("查询成功", taskInstanceDetailVO);
    }

    @Operation(
        summary = "接取任务",
        description = "接取 PENDING 状态的任务，接取后任务状态变为 CLAIMED，归属当前登录用户"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/claim")
    public Result<Void> claimTask(@PathVariable Long instanceId) {
        Long userId = sessionService.getCurrentUserId();
        taskInstanceService.claimTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(userId)
                        .build());
        return Result.success("接取成功");
    }

    @Operation(
        summary = "放弃任务",
        description = "放弃已接取（CLAIMED）的任务，任务状态恢复为 PENDING，可由他人重新接取"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/abandon")
    public Result<Void> abandonTask(@PathVariable Long instanceId) {
        Long userId = sessionService.getCurrentUserId();
        taskInstanceService.abandonTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(userId)
                        .build());
        return Result.success("已放弃");
    }

    @Operation(
        summary = "提交任务",
        description = "提交待提交（CLAIMED）的任务，提交后任务状态变为 COMPLETED。提交内容通过 metadata(JSON对象) 传递"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/submit")
    public Result<Void> submitTask(
            @PathVariable Long instanceId,
            @RequestBody @Valid TaskSubmitDTO taskSubmitDTO) {
        Long userId = sessionService.getCurrentUserId();
        taskInstanceService.submitTask(
                TaskInstanceSubmitParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(userId)
                        .metadata(taskSubmitDTO.getMetadata())
                        .build());
        return Result.success("提交成功");
    }

    @Operation(
        summary = "重置任务提交",
        description = "将已完成（COMPLETED）的任务重置为 CLAIMED 状态，允许重新提交。"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/reset")
    public Result<Void> resetTask(@PathVariable Long instanceId) {
        Long userId = sessionService.getCurrentUserId();
        taskInstanceService.resetTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(userId)
                        .build());
        return Result.success("已重置");
    }

    @Operation(
        summary = "打回任务",
        description = "下一阶段执行人可将已完成（COMPLETED）的任务打回，" +
                "任务恢复为 CLAIMED 状态，提交记录标记为 REJECTED，需填写打回意见"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/reject")
    public Result<Void> rejectTask(
            @PathVariable Long instanceId,
            @RequestBody @Valid TaskRejectDTO taskRejectDTO) {
        Long userId = sessionService.getCurrentUserId();
        taskInstanceService.rejectTask(
                TaskInstanceRejectParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(userId)
                        .reviewComment(taskRejectDTO.getReviewComment())
                        .build());
        return Result.success("已打回");
    }

}
