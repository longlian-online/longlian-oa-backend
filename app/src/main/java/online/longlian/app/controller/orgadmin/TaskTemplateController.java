package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.PageParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateDetailResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateNodeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateNodeResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateUpdateParamsBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.TaskTemplateCreateDTO;
import online.longlian.app.pojo.dto.orgadmin.TaskTemplateListDTO;
import online.longlian.app.pojo.dto.orgadmin.TaskTemplateNodeDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.TaskTemplateDetailVO;
import online.longlian.app.pojo.vo.orgadmin.TaskTemplateListVO;
import online.longlian.app.pojo.vo.orgadmin.TaskTemplateNodeVO;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.app.service.orgadmin.TaskTemplateService;
import online.longlian.app.service.user.SessionService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "任务模板管理", description = "任务流模板的增删改查与状态管理，仅管理员可操作")
@RequestMapping("/orgadmin/task/template")
@RestController
@RequiredArgsConstructor
public class TaskTemplateController {

    private final TaskTemplateService taskTemplateService;
    private final SessionService sessionService;
    private final CurrentOrganizationService currentOrganizationService;

    @Operation(
        summary = "分页查询任务模板列表",
        description = "支持名称模糊搜索、状态筛选、创建时间区间；支持按创建时间或引用次数排序，默认按引用次数倒序"
    )
    @PostMapping("/list")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<PageResultVO<TaskTemplateListVO>> listTaskTemplates(
            @RequestBody @Valid TaskTemplateListDTO taskTemplateListDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        PageResultBO<TaskTemplateListResultBO> resultBO = taskTemplateService.listTaskTemplates(
                TaskTemplateListParamsBO.builder()
                        .orgId(orgId)
                        .keyword(taskTemplateListDTO.getKeyword())
                        .status(taskTemplateListDTO.getStatus())
                        .startCreatedTime(taskTemplateListDTO.getStartCreatedTime())
                        .endCreatedTime(taskTemplateListDTO.getEndCreatedTime())
                        .sortBy(taskTemplateListDTO.getSortBy())
                        .orderDir(taskTemplateListDTO.getOrderDir())
                        .page(new PageParamsBO(taskTemplateListDTO.getPageNum(), taskTemplateListDTO.getPageSize()))
                        .build()
        );

        List<TaskTemplateListVO> taskTemplateListVOList = resultBO.getList().stream().map(bo -> {
            TaskTemplateListVO taskTemplateListVO = new TaskTemplateListVO();
            BeanUtils.copyProperties(bo, taskTemplateListVO);
            return taskTemplateListVO;
        }).toList();

        return Result.success("查询成功", new PageResultVO<>(taskTemplateListVOList, resultBO.getTotal()));
    }

    @Operation(
        summary = "获取任务模板详情",
        description = "返回模板基本信息及完整节点列表"
    )
    @Parameter(name = "templateId", description = "任务模板ID")
    @GetMapping("/{templateId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<TaskTemplateDetailVO> getTaskTemplateDetail(@PathVariable Long templateId) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        TaskTemplateDetailResultBO resultBO = taskTemplateService.getTaskTemplateDetail(templateId, orgId);

        TaskTemplateDetailVO taskTemplateDetailVO = new TaskTemplateDetailVO();
        BeanUtils.copyProperties(resultBO, taskTemplateDetailVO, "nodes");
        List<TaskTemplateNodeVO> taskTemplateNodeVOList = resultBO.getNodes().stream()
                .map(nodeBO -> {
                    TaskTemplateNodeVO taskTemplateNodeVO = new TaskTemplateNodeVO();
                    BeanUtils.copyProperties(nodeBO, taskTemplateNodeVO);
                    return taskTemplateNodeVO;
                })
                .toList();
        taskTemplateDetailVO.setNodes(taskTemplateNodeVOList);

        return Result.success("查询成功", taskTemplateDetailVO);
    }

    @Operation(summary = "创建任务模板", description = "同时创建模板基本信息与节点列表")
    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> createTaskTemplate(
            @RequestBody @Valid TaskTemplateCreateDTO taskTemplateCreateDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        List<TaskTemplateNodeCreateParamsBO> nodeBOs = taskTemplateCreateDTO.getNodes().stream()
                .map(nodeDTO -> TaskTemplateNodeCreateParamsBO.builder()
                        .baseTaskId(nodeDTO.getBaseTaskId())
                        .sort(nodeDTO.getSort())
                        .parallelSort(nodeDTO.getParallelSort())
                        .build())
                .toList();

        taskTemplateService.createTaskTemplate(
                TaskTemplateCreateParamsBO.builder()
                        .orgId(orgId)
                        .creatorId(userId)
                        .name(taskTemplateCreateDTO.getName())
                        .description(taskTemplateCreateDTO.getDescription())
                        .nodes(nodeBOs)
                        .build()
        );
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
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        List<TaskTemplateNodeCreateParamsBO> nodeBOs = taskTemplateCreateDTO.getNodes().stream()
                .map(nodeDTO -> TaskTemplateNodeCreateParamsBO.builder()
                        .baseTaskId(nodeDTO.getBaseTaskId())
                        .sort(nodeDTO.getSort())
                        .parallelSort(nodeDTO.getParallelSort())
                        .build())
                .toList();

        taskTemplateService.updateTaskTemplate(
                TaskTemplateUpdateParamsBO.builder()
                        .templateId(templateId)
                        .orgId(orgId)
                        .name(taskTemplateCreateDTO.getName())
                        .description(taskTemplateCreateDTO.getDescription())
                        .nodes(nodeBOs)
                        .build()
        );
        return Result.success("更新成功");
    }

    @Operation(
            summary = "启用/禁用任务模板",
            description = "禁用后无法基于该模板创建新任务流，已有任务流不受影响。status: ENABLED-启用，DISABLED-禁用"
    )
    @PatchMapping("/{templateId}/status")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> changeTaskTemplateStatus(@PathVariable Long templateId, @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        taskTemplateService.changeTaskTemplateStatus(
                TaskTemplateChangeStatusParamsBO.builder()
                        .templateId(templateId)
                        .orgId(orgId)
                        .status(changeStatusDTO.getStatus())
                        .build()
        );
        return Result.success(null);
    }
}
