package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.PageParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.app.ProjectCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectDetailResultBO;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectListResultBO;
import online.longlian.app.pojo.bo.app.ProjectUpdateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopRemoveParamsBO;
import online.longlian.app.pojo.dto.app.ProjectCreateDTO;
import online.longlian.app.pojo.dto.app.ProjectListDTO;
import online.longlian.app.pojo.dto.app.ProjectUpdateDTO;
import online.longlian.app.pojo.vo.app.ProjectDetailInfoVO;
import online.longlian.app.pojo.vo.app.ProjectInfoVO;
import online.longlian.app.pojo.vo.app.ProjectTypeInfoVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.common.CurrentOrganizationService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "企划接口", description = "企划相关接口")
@RequestMapping("/app/projects")
@RequiredArgsConstructor
@RestController
public class ProjectController {

    private final ProjectService projectService;
    private final SessionService sessionService;
    private final CurrentOrganizationService currentOrganizationService;

    @Operation(summary = "分页查询企划列表", description = "支持关键词搜索、类型筛选、排序，仅返回启用状态企划")
    @GetMapping("")
    public Result<PageResultVO<ProjectInfoVO>> getProjectList(
            @ModelAttribute @Valid ProjectListDTO projectListDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        PageResultBO<ProjectListResultBO> resultBO = projectService.getProjectList(
                ProjectListParamsBO.builder()
                        .orgId(orgId)
                        .keyword(projectListDTO.getKeyword())
                        .projectType(projectListDTO.getProjectType())
                        .sortByTime(projectListDTO.getSortByTime())
                        .orderDir(projectListDTO.getOrderDir())
                        .page(new PageParamsBO(projectListDTO.getPageNum(), projectListDTO.getPageSize()))
                        .build());

        List<ProjectInfoVO> voList = resultBO.getList().stream().map(bo -> {
            ProjectInfoVO vo = new ProjectInfoVO();
            BeanUtils.copyProperties(bo, vo);
            return vo;
        }).toList();

        return Result.success("查询成功", new PageResultVO<>(voList, resultBO.getTotal()));
    }

    @Operation(
        summary = "获取企划详情",
        description = """
                返回企划详细信息、进度统计及当前用户权限标记。
                isCreator=true 时前端展示「编辑+分享」按钮；false 时展示「添加到工坊+分享」按钮
                """
    )
    @Parameter(name = "projectId", description = "企划ID")
    @GetMapping("/{projectId}")
    public Result<ProjectDetailInfoVO> getProjectDetail(@PathVariable Long projectId) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        ProjectDetailResultBO resultBO = projectService.getProjectDetail(projectId, userId, orgId);

        ProjectDetailInfoVO vo = new ProjectDetailInfoVO();
        BeanUtils.copyProperties(resultBO, vo);
        return Result.success("查询成功", vo);
    }

    @Operation(summary = "获取企划类型列表", description = "仅返回启用状态的类型")
    @GetMapping("/types")
    public Result<List<ProjectTypeInfoVO>> getProjectTypes() {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        List<ProjectTypeInfoVO> types = projectService.getProjectTypes(orgId);
        return Result.success("查询成功", types);
    }

    @Operation(summary = "创建企划")
    @PostMapping("")
    public Result<Void> createProject(
            @RequestBody @Valid ProjectCreateDTO projectCreateDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        projectService.createProject(
                ProjectCreateParamsBO.builder()
                        .orgId(orgId)
                        .creatorId(userId)
                        .title(projectCreateDTO.getTitle())
                        .alias(projectCreateDTO.getAlias())
                        .typeId(projectCreateDTO.getTypeId())
                        .metadata(projectCreateDTO.getMetadata())
                        .description(projectCreateDTO.getDescription())
                        .coverFileId(projectCreateDTO.getCoverFileId())
                        .build());
        return Result.success("创建成功");
    }

    @Operation(summary = "编辑企划")
    @Parameter(name = "projectId", description = "企划ID")
    @PutMapping("/{projectId}")
    public Result<Void> updateProject(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectUpdateDTO projectUpdateDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        projectService.updateProject(
                ProjectUpdateParamsBO.builder()
                        .projectId(projectId)
                        .orgId(orgId)
                        .userId(userId)
                        .title(projectUpdateDTO.getTitle())
                        .alias(projectUpdateDTO.getAlias())
                        .metadata(projectUpdateDTO.getMetadata())
                        .description(projectUpdateDTO.getDescription())
                        .coverFileId(projectUpdateDTO.getCoverFileId())
                        .build());
        return Result.success("修改成功");
    }

    @Operation(
            summary = "添加企划到工坊",
            description = "将指定企划加入当前用户的个人工坊，已添加则幂等返回成功"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @PostMapping("/{projectId}/workshop")
    public Result<Void> addToWorkshop(@PathVariable Long projectId) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        projectService.addToWorkshop(
                ProjectWorkshopAddParamsBO.builder()
                        .projectId(projectId)
                        .userId(userId)
                        .orgId(orgId)
                        .build());
        return Result.success("已添加到工坊");
    }

    @Operation(
            summary = "从工坊移除企划",
            description = "将指定企划从当前用户的个人工坊中移除"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @DeleteMapping("/{projectId}/workshop")
    public Result<Void> removeFromWorkshop(@PathVariable Long projectId) {
        Long userId = sessionService.getCurrentUserId();

        projectService.removeFromWorkshop(
                ProjectWorkshopRemoveParamsBO.builder()
                        .projectId(projectId)
                        .userId(userId)
                        .build());
        return Result.success("已从工坊移除");
    }
}
