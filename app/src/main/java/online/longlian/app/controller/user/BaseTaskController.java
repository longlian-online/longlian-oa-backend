package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.BaseTaskCreateDTO;
import online.longlian.app.pojo.dto.BaseTaskListDTO;
import online.longlian.app.pojo.vo.BaseTaskVO;
import online.longlian.app.pojo.vo.PageResultVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "原子任务管理接口", description = "原子任务（最小任务单元）管理；任务创建后不可编辑，仅支持启用/禁用")
@RequestMapping("/app/task/base")
@RestController
@RequiredArgsConstructor
public class BaseTaskController {

    // private final BaseTaskService baseTaskService;

    @Operation(
        summary = "分页查询原子任务列表",
        description = "支持名称模糊搜索、状态筛选、创建时间区间；支持按创建时间或引用次数排序，默认按引用次数倒序"
    )
    @PostMapping("/list")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<PageResultVO<BaseTaskVO>> listBaseTasks(
            @RequestBody @Valid BaseTaskListDTO baseTaskListDTO) {
        // TODO
        // return baseTaskService.listBaseTasks(baseTaskListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(
        summary = "获取全部启用原子任务",
        description = "返回当前组织下所有启用状态的原子任务，不分页"
    )
    @GetMapping("/all")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<List<BaseTaskVO>> listAllEnabledBaseTasks() {
        // TODO
        // return baseTaskService.listAllEnabledBaseTasks();
        return Result.success("查询成功", null);
    }

    @Operation(
        summary = "创建原子任务",
        description = "任务创建后不可编辑，请确认字段后提交"
    )
    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> createBaseTask(
            @RequestBody @Valid BaseTaskCreateDTO baseTaskCreateDTO) {
        // TODO
        // return baseTaskService.createBaseTask(baseTaskCreateDTO);
        return Result.success("创建成功");
    }

    @Operation(
        summary = "启用/禁用原子任务",
        description = "禁用后该任务无法被添加到新模板节点中，已引用的节点不受影响。status: 1-启用，0-禁用"
    )
    @Parameter(name = "baseTaskId", description = "原子任务ID")
    @Parameter(name = "status", description = "目标状态：1-启用，0-禁用")
    @PatchMapping("/{baseTaskId}/status")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> changeBaseTaskStatus(
            @PathVariable Long baseTaskId,
            @RequestParam Integer status) {
        // TODO
        // return baseTaskService.changeBaseTaskStatus(baseTaskId, status);
        return Result.success(null);
    }
}
