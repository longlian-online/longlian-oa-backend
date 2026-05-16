package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.app.TaskRejectDTO;
import online.longlian.app.pojo.dto.app.TaskSubmitDTO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "任务实例接口", description = "任务接取、放弃、提交、重置、打回、查看详情")
@RequestMapping("/app/task/instance")
@RestController
@RequiredArgsConstructor
public class TaskInstanceController {

    // private final TaskInstanceService taskInstanceService;

    // -------------------------
    // 任务操作
    // -------------------------

    @Operation(
        summary = "接取任务",
        description = "接取 PENDING 状态的任务，接取后任务状态变为 CLAIMED，归属当前登录用户"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/claim")
    public Result<Void> claimTask(@PathVariable Long instanceId) {
        // TODO
        // return taskInstanceService.claimTask(instanceId);
        return Result.success("接取成功");
    }

    @Operation(
        summary = "放弃任务",
        description = "放弃已接取（CLAIMED）的任务，任务状态恢复为 PENDING，可由他人重新接取"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/abandon")
    public Result<Void> abandonTask(@PathVariable Long instanceId) {
        // TODO
        // return taskInstanceService.abandonTask(instanceId);
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
        // TODO
        // return taskInstanceService.submitTask(instanceId, taskSubmitDTO);
        return Result.success("提交成功");
    }

    @Operation(
        summary = "重置任务提交",
        description = "将已完成（COMPLETED）的任务重置为 CLAIMED 状态，允许重新提交。"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/reset")
    public Result<Void> resetTask(@PathVariable Long instanceId) {
        // TODO
        // return taskInstanceService.resetTask(instanceId);
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
        // TODO
        // return taskInstanceService.rejectTask(instanceId, taskRejectDTO);
        return Result.success("已打回");
    }

    @Operation(
        summary = "查看任务实例详情",
        description = "返回任务实例信息及最近一次提交的元数据详情"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @GetMapping("/{instanceId}/detail")
    public Result<TaskInstanceDetailVO> getTaskInstanceDetail(@PathVariable Long instanceId) {
        // TODO
        // return taskInstanceService.getTaskInstanceDetail(instanceId);
        return Result.success("查询成功", null);
    }
}
