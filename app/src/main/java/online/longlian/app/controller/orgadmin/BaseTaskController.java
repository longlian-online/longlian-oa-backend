package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.PageParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListResultBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.BaseTaskCreateDTO;
import online.longlian.app.pojo.dto.orgadmin.BaseTaskListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.BaseTaskVO;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.app.service.orgadmin.BaseTaskService;
import online.longlian.app.service.user.SessionService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "原子任务管理", description = "原子任务（最小任务单元）管理；任务创建后不可编辑，仅支持启用/禁用")
@RequestMapping("/orgadmin/task/base")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class BaseTaskController {

    private final BaseTaskService baseTaskService;
    private final SessionService sessionService;
    private final CurrentOrganizationService currentOrganizationService;

    @Operation(
        summary = "分页查询原子任务列表",
        description = "支持名称模糊搜索、状态筛选、创建时间区间；支持按创建时间或引用次数排序，默认按引用次数倒序"
    )
    @PostMapping("/list")
    public Result<PageResultVO<BaseTaskVO>> listBaseTasks(
            @RequestBody @Valid BaseTaskListDTO baseTaskListDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        PageResultBO<BaseTaskListResultBO> resultBO = baseTaskService.listBaseTasks(
                BaseTaskListParamsBO.builder()
                        .orgId(orgId)
                        .keyword(baseTaskListDTO.getKeyword())
                        .status(baseTaskListDTO.getStatus())
                        .startCreatedTime(baseTaskListDTO.getStartCreatedTime())
                        .endCreatedTime(baseTaskListDTO.getEndCreatedTime())
                        .sortBy(baseTaskListDTO.getSortBy())
                        .orderDir(baseTaskListDTO.getOrderDir())
                        .page(new PageParamsBO(baseTaskListDTO.getPageNum(), baseTaskListDTO.getPageSize()))
                        .build()
        );

        List<BaseTaskVO> baseTaskVOList = resultBO.getList().stream().map(bo -> {
            BaseTaskVO baseTaskVO = new BaseTaskVO();
            BeanUtils.copyProperties(bo, baseTaskVO);
            return baseTaskVO;
        }).toList();

        return Result.success("查询成功", new PageResultVO<>(baseTaskVOList, resultBO.getTotal()));
    }

    @Operation(
        summary = "创建原子任务",
        description = "任务创建后不可编辑，请确认标题、图标、简介和元数据字段定义后提交"
    )
    @PostMapping
    public Result<Void> createBaseTask(
            @RequestBody @Valid BaseTaskCreateDTO baseTaskCreateDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        baseTaskService.createBaseTask(
                BaseTaskCreateParamsBO.builder()
                        .orgId(orgId)
                        .creatorId(userId)
                        .name(baseTaskCreateDTO.getName())
                        .description(baseTaskCreateDTO.getDescription())
                        .iconFileId(baseTaskCreateDTO.getIconFileId())
                        .metaSchema(baseTaskCreateDTO.getMetaSchema())
                        .build()
        );
        return Result.success("创建成功");
    }

    @Operation(
            summary = "启用/禁用原子任务",
            description = "禁用后该任务无法被添加到新模板节点中，已引用的节点不受影响。status: ENABLED-启用，DISABLED-禁用"
    )
    @PatchMapping("/{taskId}/status")
    public Result<Void> changeBaseTaskStatus(@PathVariable Long taskId, @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        baseTaskService.changeBaseTaskStatus(
                BaseTaskChangeStatusParamsBO.builder()
                        .taskId(taskId)
                        .orgId(orgId)
                        .status(changeStatusDTO.getStatus())
                        .build()
        );
        return Result.success(changeStatusDTO.getStatus().getDesc() + "成功");
    }
}
