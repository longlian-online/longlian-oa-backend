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
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
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

    @Operation(summary = "分页查询企划列表", description = "支持关键词搜索、类型筛选、排序，仅返回启用状态企划")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public PageResultVO<ProjectInfoVO> getProjectList(
            @UserSession SessionContext sessionContext,
            @ModelAttribute @Valid ProjectListDTO projectListDTO) {
        PageResultBO<ProjectListResultBO> resultBO = projectService.getProjectList(
                ProjectListParamsBO.builder()
                        .orgId(sessionContext.orgId())
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

        return new PageResultVO<>(voList, resultBO.getTotal());
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
    @ResponseMessage("查询成功")
    public ProjectDetailInfoVO getProjectDetail(@UserSession SessionContext sessionContext,
                                                 @PathVariable Long projectId) {
        ProjectDetailResultBO resultBO = projectService.getProjectDetail(
                projectId, sessionContext.userId(), sessionContext.orgId());
        ProjectDetailInfoVO vo = new ProjectDetailInfoVO();
        BeanUtils.copyProperties(resultBO, vo);
        return vo;
    }

    @Operation(summary = "获取企划类型列表", description = "仅返回启用状态的类型")
    @GetMapping("/types")
    @ResponseMessage("查询成功")
    public List<ProjectTypeInfoVO> getProjectTypes(@UserSession SessionContext sessionContext) {
        return projectService.getProjectTypes(sessionContext.orgId());
    }

    @Operation(summary = "创建企划")
    @PostMapping("")
    @ResponseMessage("创建成功")
    public void createProject(@UserSession SessionContext sessionContext,
                               @RequestBody @Valid ProjectCreateDTO projectCreateDTO) {
        projectService.createProject(
                ProjectCreateParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .creatorId(sessionContext.userId())
                        .title(projectCreateDTO.getTitle())
                        .alias(projectCreateDTO.getAlias())
                        .typeId(projectCreateDTO.getTypeId())
                        .metadata(projectCreateDTO.getMetadata())
                        .description(projectCreateDTO.getDescription())
                        .coverFileId(projectCreateDTO.getCoverFileId())
                        .build());
    }

    @Operation(summary = "编辑企划")
    @Parameter(name = "projectId", description = "企划ID")
    @PutMapping("/{projectId}")
    @ResponseMessage("修改成功")
    public void updateProject(@UserSession SessionContext sessionContext,
                               @PathVariable Long projectId,
                               @RequestBody @Valid ProjectUpdateDTO projectUpdateDTO) {
        projectService.updateProject(
                ProjectUpdateParamsBO.builder()
                        .projectId(projectId)
                        .orgId(sessionContext.orgId())
                        .userId(sessionContext.userId())
                        .title(projectUpdateDTO.getTitle())
                        .alias(projectUpdateDTO.getAlias())
                        .metadata(projectUpdateDTO.getMetadata())
                        .description(projectUpdateDTO.getDescription())
                        .coverFileId(projectUpdateDTO.getCoverFileId())
                        .build());
    }

    @Operation(
            summary = "添加企划到工坊",
            description = "将指定企划加入当前用户的个人工坊，已添加则幂等返回成功"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @PostMapping("/{projectId}/workshop")
    @ResponseMessage("已添加到工坊")
    public void addToWorkshop(@UserSession SessionContext sessionContext,
                               @PathVariable Long projectId) {
        projectService.addToWorkshop(
                ProjectWorkshopAddParamsBO.builder()
                        .projectId(projectId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .build());
    }

    @Operation(
            summary = "从工坊移除企划",
            description = "将指定企划从当前用户的个人工坊中移除"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @DeleteMapping("/{projectId}/workshop")
    @ResponseMessage("已从工坊移除")
    public void removeFromWorkshop(@UserSession SessionContext sessionContext,
                                    @PathVariable Long projectId) {
        projectService.removeFromWorkshop(
                ProjectWorkshopRemoveParamsBO.builder()
                        .projectId(projectId)
                        .userId(sessionContext.userId())
                        .build());
    }
}
