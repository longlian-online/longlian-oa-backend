package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.TaskRejectDTO;
import online.longlian.app.pojo.dto.TaskSubmitDTO;
import online.longlian.app.pojo.vo.PageResultVO;
import online.longlian.app.pojo.vo.TaskInstanceVO;
import online.longlian.app.pojo.vo.TaskSubmissionVO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "任务实例接口", description = "任务接取、放弃、提交、重置、打回、下载")
@RequestMapping("/app/task/instance")
@RestController
@RequiredArgsConstructor
public class TaskInstanceController {

    // private final TaskInstanceService taskInstanceService;

    // -------------------------
    // 任务列表
    // -------------------------

    @Operation(
        summary = "查询企划下可执行的任务列表",
        description = "返回当前用户在指定企划下可见的任务实例列表（PENDING/CLAIMED 状态）"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @GetMapping("/project/{projectId}")
    public Result<List<TaskInstanceVO>> listProjectTasks(@PathVariable Long projectId) {
        // TODO
        // return taskInstanceService.listProjectTasks(projectId);
        return Result.success("查询成功", null);
    }

    // -------------------------
    // 提交记录列表
    // -------------------------

    @Operation(
        summary = "查询任务提交记录列表",
        description = "查询指定任务实例的所有提交记录，按提交时间倒序排列"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @GetMapping("/{instanceId}/submissions")
    public Result<PageResultVO<TaskSubmissionVO>> listSubmissions(
            @PathVariable Long instanceId) {
        // TODO
        // return taskInstanceService.listSubmissions(instanceId);
        return Result.success("查询成功", null);
    }

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
        description = "提交已接取（CLAIMED）的任务，提交后任务状态变为 COMPLETED。" +
                "若原子任务要求上传文件（needFile=1），fileId 必须传入"
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
        summary = "下载任务成果文件",
        description = "下载指定提交记录的上传文件"
    )
    @Parameter(name = "submissionId", description = "提交记录ID")
    @GetMapping("/submission/{submissionId}/download")
    public ResponseEntity<Resource> downloadSubmissionFile(
            @PathVariable Long submissionId) {
        // TODO
        // return taskInstanceService.downloadSubmissionFile(submissionId);
        return ResponseEntity.ok().build();
    }
}
