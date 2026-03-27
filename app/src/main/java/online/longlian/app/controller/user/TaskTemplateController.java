package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.TaskTemplateCreateDTO;
import online.longlian.app.pojo.dto.TaskTemplateListDTO;
import online.longlian.app.pojo.vo.PageResultVO;
import online.longlian.app.pojo.vo.TaskTemplateDetailVO;
import online.longlian.app.pojo.vo.TaskTemplateListVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "任务模板管理接口", description = "任务流模板的增删改查与状态管理，仅管理员可操作")
@RequestMapping("/app/task/template")
@RestController
@RequiredArgsConstructor
public class TaskTemplateController {

    // private final TaskTemplateService taskTemplateService;

    @Operation(
        summary = "分页查询任务模板列表",
        description = "支持名称模糊搜索、状态筛选、创建时间区间；支持按创建时间或引用次数排序，默认按引用次数倒序"
    )
    @PostMapping("/list")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<PageResultVO<TaskTemplateListVO>> listTaskTemplates(
            @RequestBody @Valid TaskTemplateListDTO taskTemplateListDTO) {
        // TODO
        // return taskTemplateService.listTaskTemplates(taskTemplateListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(
        summary = "获取任务模板详情",
        description = "返回模板基本信息及完整节点列表"
    )
    @Parameter(name = "templateId", description = "任务模板ID")
    @GetMapping("/{templateId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<TaskTemplateDetailVO> getTaskTemplateDetail(@PathVariable Long templateId) {
        // TODO
        // return taskTemplateService.getTaskTemplateDetail(templateId);
        return Result.success("查询成功", null);
    }

    @Operation(summary = "创建任务模板", description = "同时创建模板基本信息与节点列表")
    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> createTaskTemplate(
            @RequestBody @Valid TaskTemplateCreateDTO taskTemplateCreateDTO) {
        // TODO
        // return taskTemplateService.createTaskTemplate(taskTemplateCreateDTO);
        return Result.success("创建成功");
    }

    @Operation(
        summary = "更新任务模板",
        description = "覆盖更新模板信息及节点列表"
    )
    @Parameter(name = "templateId", description = "任务模板ID")
    @PutMapping("/{templateId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> updateTaskTemplate(
            @PathVariable Long templateId,
            @RequestBody @Valid TaskTemplateCreateDTO taskTemplateCreateDTO) {
        // TODO
        // return taskTemplateService.updateTaskTemplate(templateId, taskTemplateCreateDTO);
        return Result.success("更新成功");
    }

    @Operation(
        summary = "启用/禁用任务模板",
        description = "禁用后无法基于该模板创建新任务流，已有任务流不受影响。status: 1-启用，0-禁用"
    )
    @Parameter(name = "templateId", description = "任务模板ID")
    @Parameter(name = "status", description = "目标状态：1-启用，0-禁用")
    @PatchMapping("/{templateId}/status")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> changeTaskTemplateStatus(
            @PathVariable Long templateId,
            @RequestParam Integer status) {
        // TODO
        // return taskTemplateService.changeTaskTemplateStatus(templateId, status);
        return Result.success(null);
    }
}
