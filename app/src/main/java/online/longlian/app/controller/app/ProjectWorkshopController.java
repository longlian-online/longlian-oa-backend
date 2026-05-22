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
import online.longlian.app.pojo.bo.app.WorkshopListParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateCreateParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateListParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateNodeCreateParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateUpdateParamsBO;
import online.longlian.app.pojo.dto.app.WorkshopListDTO;
import online.longlian.app.pojo.dto.app.WorkshopTaskTemplateCreateDTO;
import online.longlian.app.pojo.dto.app.WorkshopTaskTemplateDTO;
import online.longlian.app.pojo.vo.app.WorkshopProjectInfoVO;
import online.longlian.app.pojo.vo.app.WorkshopTaskTemplateVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.app.ProjectWorkshopService;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.common.CurrentOrganizationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "工坊接口", description = "用户个人工坊：企划列表、任务流模板列表与创建")
@RequestMapping("/app/workshop")
@RestController
@RequiredArgsConstructor
public class ProjectWorkshopController {

    private final ProjectWorkshopService projectWorkshopService;
    private final SessionService sessionService;
    private final CurrentOrganizationService currentOrganizationService;

    @Operation(
        summary = "分页查询工坊企划列表",
        description = "查询当前用户工坊中的企划列表，支持标题模糊搜索、类型筛选、仅看我创建，默认按添加时间倒序。"
    )
    @PostMapping("/list")
    public Result<PageResultVO<WorkshopProjectInfoVO>> getMyWorkshopList(
            @RequestBody @Valid WorkshopListDTO workshopListDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        PageResultBO<WorkshopProjectInfoVO> resultBO = projectWorkshopService.getMyWorkshopList(
                WorkshopListParamsBO.builder()
                        .userId(userId)
                        .orgId(orgId)
                        .keyword(workshopListDTO.getKeyword())
                        .projectType(workshopListDTO.getProjectType())
                        .isMyCreated(workshopListDTO.getIsMyCreated())
                        .page(new PageParamsBO(workshopListDTO.getPageNum(), workshopListDTO.getPageSize()))
                        .build());

        return Result.success("查询成功", new PageResultVO<>(resultBO.getList(), resultBO.getTotal()));
    }

    @Operation(
            summary = "分页查询工坊任务流模板列表",
            description = "查询当前用户工坊中的任务流模板（组织通用模板+我的个人模板），支持模板名称模糊搜索、仅看我创建，默认按创建时间倒序。"
    )
    @PostMapping("/task-template/list")
    public Result<PageResultVO<WorkshopTaskTemplateVO>> getWorkshopTaskTemplateList(
            @RequestBody @Valid WorkshopTaskTemplateDTO workshopTaskTemplateDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        PageResultBO<WorkshopTaskTemplateVO> resultBO = projectWorkshopService.getWorkshopTaskTemplateList(
                WorkshopTaskTemplateListParamsBO.builder()
                        .orgId(orgId)
                        .userId(userId)
                        .keyword(workshopTaskTemplateDTO.getKeyword())
                        .isMyCreated(workshopTaskTemplateDTO.getIsMyCreated())
                        .page(new PageParamsBO(workshopTaskTemplateDTO.getPageNum(), workshopTaskTemplateDTO.getPageSize()))
                        .build());

        return Result.success("查询成功", new PageResultVO<>(resultBO.getList(), resultBO.getTotal()));
    }

    @Operation(
            summary = "创建工坊个人任务流模板",
            description = "用户在可视化编辑器中搭建节点结构后保存为个人任务流模板，scope 固定为 PERSONAL。"
    )
    @PostMapping("/task-template")
    public Result<Void> createWorkshopTaskTemplate(
            @RequestBody @Valid WorkshopTaskTemplateCreateDTO workshopTaskTemplateCreateDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        List<WorkshopTaskTemplateNodeCreateParamsBO> nodeBOs = workshopTaskTemplateCreateDTO.getNodes().stream()
                .map(nodeDTO -> WorkshopTaskTemplateNodeCreateParamsBO.builder()
                        .baseTaskId(nodeDTO.getBaseTaskId())
                        .customName(nodeDTO.getCustomName())
                        .sort(nodeDTO.getSort())
                        .parallelSort(nodeDTO.getParallelSort())
                        .build())
                .toList();

        projectWorkshopService.createWorkshopTaskTemplate(
                WorkshopTaskTemplateCreateParamsBO.builder()
                        .orgId(orgId)
                        .creatorId(userId)
                        .name(workshopTaskTemplateCreateDTO.getName())
                        .description(workshopTaskTemplateCreateDTO.getDescription())
                        .nodes(nodeBOs)
                        .build());
        return Result.success("创建成功");
    }

    @Operation(
            summary = "更新工坊个人任务流模板",
            description = "仅模板创建者可更新自己的个人模板（isMine=true 时前端展示更新按钮）。组织通用模板不可在此更新。"
    )
    @Parameter(name = "templateId", description = "任务流模板ID")
    @PutMapping("/task-template/{templateId}")
    public Result<Void> updateWorkshopTaskTemplate(
            @PathVariable Long templateId,
            @RequestBody @Valid WorkshopTaskTemplateCreateDTO workshopTaskTemplateCreateDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        List<WorkshopTaskTemplateNodeCreateParamsBO> nodeBOs = workshopTaskTemplateCreateDTO.getNodes().stream()
                .map(nodeDTO -> WorkshopTaskTemplateNodeCreateParamsBO.builder()
                        .baseTaskId(nodeDTO.getBaseTaskId())
                        .customName(nodeDTO.getCustomName())
                        .sort(nodeDTO.getSort())
                        .parallelSort(nodeDTO.getParallelSort())
                        .build())
                .toList();

        projectWorkshopService.updateWorkshopTaskTemplate(
                WorkshopTaskTemplateUpdateParamsBO.builder()
                        .templateId(templateId)
                        .orgId(orgId)
                        .userId(userId)
                        .name(workshopTaskTemplateCreateDTO.getName())
                        .description(workshopTaskTemplateCreateDTO.getDescription())
                        .nodes(nodeBOs)
                        .build());
        return Result.success("更新成功");
    }
}
