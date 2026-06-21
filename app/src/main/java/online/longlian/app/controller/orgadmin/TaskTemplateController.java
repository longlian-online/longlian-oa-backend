package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateDetailResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateNodeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateUpdateParamsBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.TaskTemplateCreateDTO;
import online.longlian.app.pojo.dto.orgadmin.TaskTemplateListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.TaskTemplateDetailVO;
import online.longlian.app.pojo.vo.orgadmin.TaskTemplateListVO;
import online.longlian.app.pojo.vo.orgadmin.TaskTemplateNodeVO;
import online.longlian.app.service.orgadmin.TaskTemplateService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "任务模板管理", description = "任务流模板的增删改查与状态管理，仅管理员可操作")
@RequestMapping("/orgadmin/task/template")
@RestController("orgAdminTaskTemplateController")
@RequiredArgsConstructor
public class TaskTemplateController {

    private final TaskTemplateService taskTemplateService;

    @Operation(
        summary = "分页查询任务模板列表",
        description = "支持名称模糊搜索、状态筛选、创建时间区间；支持按创建时间或引用次数排序，默认按引用次数倒序"
    )
    @PostMapping("/list")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @ResponseMessage("查询成功")
    public PageResultVO<TaskTemplateListVO> listTaskTemplates(
            @UserSession(required = true) SessionContext sessionContext,
            @RequestBody @Valid TaskTemplateListDTO taskTemplateListDTO) {
        PageResultBO<TaskTemplateListResultBO> resultBO = taskTemplateService.listTaskTemplates(
                TaskTemplateListParamsBO.builder()
                        .orgId(sessionContext.orgId())
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

        return new PageResultVO<>(taskTemplateListVOList, resultBO.getTotal());
    }

    @Operation(
        summary = "获取任务模板详情",
        description = "返回模板基本信息及完整节点列表"
    )
    @Parameter(name = "templateId", description = "任务模板ID")
    @GetMapping("/{templateId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @ResponseMessage("查询成功")
    public TaskTemplateDetailVO getTaskTemplateDetail(
            @UserSession(required = true) SessionContext sessionContext,
            @PathVariable Long templateId) {
        TaskTemplateDetailResultBO resultBO = taskTemplateService.getTaskTemplateDetail(
                templateId, sessionContext.orgId());

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

        return taskTemplateDetailVO;
    }

    @Operation(summary = "创建任务模板", description = "同时创建模板基本信息与节点列表")
    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @ResponseMessage("创建成功")
    public void createTaskTemplate(@UserSession(required = true) SessionContext sessionContext,
                                    @RequestBody @Valid TaskTemplateCreateDTO taskTemplateCreateDTO) {
        List<TaskTemplateNodeCreateParamsBO> nodeBOs = taskTemplateCreateDTO.getNodes().stream()
                .map(nodeDTO -> TaskTemplateNodeCreateParamsBO.builder()
                        .baseTaskId(nodeDTO.getBaseTaskId())
                        .sort(nodeDTO.getSort())
                        .parallelSort(nodeDTO.getParallelSort())
                        .build())
                .toList();

        taskTemplateService.createTaskTemplate(
                TaskTemplateCreateParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .creatorId(sessionContext.userId())
                        .name(taskTemplateCreateDTO.getName())
                        .description(taskTemplateCreateDTO.getDescription())
                        .nodes(nodeBOs)
                        .build()
        );
    }

    @Operation(
        summary = "更新任务模板",
        description = "覆盖更新模板信息及节点列表"
    )
    @Parameter(name = "templateId", description = "任务模板ID")
    @PutMapping("/{templateId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @ResponseMessage("更新成功")
    public void updateTaskTemplate(@UserSession(required = true) SessionContext sessionContext,
                                    @PathVariable Long templateId,
                                    @RequestBody @Valid TaskTemplateCreateDTO taskTemplateCreateDTO) {
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
                        .orgId(sessionContext.orgId())
                        .name(taskTemplateCreateDTO.getName())
                        .description(taskTemplateCreateDTO.getDescription())
                        .nodes(nodeBOs)
                        .build()
        );
    }

    @Operation(
            summary = "启用/禁用任务模板",
            description = "禁用后无法基于该模板创建新任务流，已有任务流不受影响。status: ENABLED-启用，DISABLED-禁用"
    )
    @PatchMapping("/{templateId}/status")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> changeTaskTemplateStatus(@UserSession(required = true) SessionContext sessionContext,
                                                  @PathVariable Long templateId,
                                                  @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        taskTemplateService.changeTaskTemplateStatus(
                TaskTemplateChangeStatusParamsBO.builder()
                        .templateId(templateId)
                        .orgId(sessionContext.orgId())
                        .status(changeStatusDTO.getStatus())
                        .build()
        );
        return Result.success(null);
    }
}
