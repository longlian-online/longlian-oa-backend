package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.enumeration.Status;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.*;
import online.longlian.app.pojo.vo.PageResultVO;
import online.longlian.app.pojo.vo.ProjectAdminInfoVO;
import online.longlian.app.pojo.vo.ProjectDetailInfoVO;
import online.longlian.app.pojo.vo.ProjectInfoVO;
import online.longlian.app.pojo.vo.ProjectTypeInfoVO;
import online.longlian.app.service.user.ProjectService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "企划接口", description = "企划相关接口")
@RequestMapping("/app/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "分页查询企划列表", description = "支持关键词搜索、类型筛选、排序，仅返回启用状态企划")
    @GetMapping("")
    public Result<PageResultVO<ProjectInfoVO>> getProjectList(
            @ModelAttribute @Valid ProjectListDTO projectListDTO) {
        // TODO
        // return projectService.getProjectList(projectListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(
        summary = "获取企划详情",
        description = """
                返回企划详细信息、进度统计及当前用户权限标记。
                isAdmin=true 时前端展示「编辑+分享」按钮；false 时展示「添加到工坊+分享」按钮
                """
    )
    @Parameter(name = "projectId", description = "企划ID")
    @GetMapping("/{projectId}")
    public Result<ProjectDetailInfoVO> getProjectDetail(@PathVariable Long projectId) {
        // TODO
        // return projectService.getProjectDetail(projectId);
        return Result.success("查询成功", null);
    }

    @Operation(summary = "获取企划类型列表", description = "仅返回启用状态的类型")
    @GetMapping("/types")
    public Result<List<ProjectTypeInfoVO>> getProjectTypes() {
        // TODO
        // return projectService.getProjectTypes();
        return Result.success("查询成功", null);
    }

    @Operation(summary = "创建企划")
    @PostMapping("")
    public Result<Void> createProject(
            @RequestBody @Valid ProjectCreateDTO projectCreateDTO) {
        // TODO
        // return projectService.createProject(projectCreateDTO);
        return Result.success("创建成功");
    }

    @Operation(summary = "编辑企划")
    @Parameter(name = "projectId", description = "企划ID")
    @PutMapping("/{projectId}")
    public Result<Void> updateProject(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectUpdateDTO projectUpdateDTO) {
        // TODO
        // return projectService.updateProject(projectId, projectUpdateDTO);
        return Result.success("修改成功");
    }

    @Operation(
            summary = "添加企划到工坊",
            description = "将指定企划加入当前用户的个人工坊，已添加则幂等返回成功"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @PostMapping("/{projectId}/workshop")
    public Result<Void> addToWorkshop(@PathVariable Long projectId) {
        // TODO
        // return projectService.addToWorkshop(projectId);
        return Result.success("已添加到工坊");
    }

    @Operation(
            summary = "从工坊移除企划",
            description = "将指定企划从当前用户的个人工坊中移除"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @DeleteMapping("/{projectId}/workshop")
    public Result<Void> removeFromWorkshop(@PathVariable Long projectId) {
        // TODO
        // return projectService.removeFromWorkshop(projectId);
        return Result.success("已从工坊移除");
    }
}
