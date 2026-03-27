package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.ProjectAdminListDTO;
import online.longlian.app.pojo.dto.ProjectCreateDTO;
import online.longlian.app.pojo.dto.ProjectListDTO;
import online.longlian.app.pojo.dto.ProjectUpdateDTO;
import online.longlian.app.pojo.vo.PageResultVO;
import online.longlian.app.pojo.vo.ProjectAdminInfoVO;
import online.longlian.app.pojo.vo.ProjectInfoVO;
import online.longlian.app.pojo.vo.ProjectTypeInfoVO;
import online.longlian.app.service.user.ProjectService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "企划接口", description = "企划相关接口")
@RequestMapping("/app/project")
@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // -------------------------
    // 用户端接口
    // -------------------------

    @Operation(summary = "分页查询企划列表", description = "用户端：支持关键词搜索、类型筛选、排序，仅返回启用状态企划")
    @PostMapping("/list")
    public Result<PageResultVO<ProjectInfoVO>> getProjectList(
            @RequestBody @Valid ProjectListDTO projectListDTO) {
        // TODO
        // return projectService.getProjectList(projectListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(summary = "获取企划类型列表", description = "用户端：仅返回启用状态的类型")
    @GetMapping("/types")
    public Result<List<ProjectTypeInfoVO>> getProjectTypes() {
        // TODO
        // return projectService.getProjectTypes();
        return Result.success("查询成功", null);
    }

    // -------------------------
    // 管理员接口
    // -------------------------

    @Operation(summary = "管理端分页查询企划列表", description = "支持标题模糊搜索、类型精确筛选、创建时间区间，默认创建时间倒序")
    @PostMapping("/admin/list")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<PageResultVO<ProjectAdminInfoVO>> getAdminProjectList(
            @RequestBody @Valid ProjectAdminListDTO projectAdminListDTO) {
        // TODO
        // return projectService.getAdminProjectList(projectAdminListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(summary = "创建企划")
    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> createProject(
            @RequestBody @Valid ProjectCreateDTO projectCreateDTO) {
        // TODO
        // return projectService.createProject(projectCreateDTO);
        return Result.success("创建成功");
    }

    @Operation(summary = "编辑企划")
    @Parameter(name = "projectId", description = "企划ID")
    @PutMapping("/{projectId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> updateProject(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectUpdateDTO projectUpdateDTO) {
        // TODO
        // return projectService.updateProject(projectId, projectUpdateDTO);
        return Result.success("修改成功");
    }

    @Operation(summary = "切换企划状态", description = "在进行中/已完成/已归档之间切换。status: 1-进行中，2-已完成，3-已归档")
    @Parameter(name = "projectId", description = "企划ID")
    @Parameter(name = "status", description = "目标状态：1-进行中，2-已完成，3-已归档")
    @PatchMapping("/{projectId}/status")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> changeProjectStatus(
            @PathVariable Long projectId,
            @RequestParam Integer status) {
        // TODO
        // return projectService.changeProjectStatus(projectId, status);
        return Result.success(null);
    }
}
