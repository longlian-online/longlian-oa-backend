package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListResultBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskUpdateParamsBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.BaseTaskCreateDTO;
import online.longlian.app.pojo.dto.orgadmin.BaseTaskListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.BaseTaskVO;
import online.longlian.app.service.orgadmin.BaseTaskService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "原子任务管理", description = "原子任务（最小任务单元）的创建、编辑、启用/禁用和引用安全删除管理")
@RequestMapping("/orgadmin/task/base")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class BaseTaskController {

    private final BaseTaskService baseTaskService;

    @Operation(
        summary = "分页查询原子任务列表",
        description = "支持名称模糊搜索、状态筛选、创建时间区间；支持按创建时间或引用次数排序，默认按引用次数倒序"
    )
    @PostMapping("/list")
    @ResponseMessage("查询成功")
    public PageResultVO<BaseTaskVO> listBaseTasks(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @RequestBody @Valid BaseTaskListDTO baseTaskListDTO) {
        PageResultBO<BaseTaskListResultBO> resultBO = baseTaskService.listBaseTasks(
                BaseTaskListParamsBO.builder()
                        .orgId(sessionContext.orgId())
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

        return new PageResultVO<>(baseTaskVOList, resultBO.getTotal());
    }

    @Operation(
        summary = "创建原子任务",
        description = "创建任务标题、图标、简介和提交字段定义；创建后支持编辑"
    )
    @PostMapping
    @ResponseMessage("创建成功")
    public void createBaseTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                @RequestBody @Valid BaseTaskCreateDTO baseTaskCreateDTO) {
        baseTaskService.createBaseTask(
                BaseTaskCreateParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .creatorId(sessionContext.userId())
                        .name(baseTaskCreateDTO.getName())
                        .description(baseTaskCreateDTO.getDescription())
                        .icon(baseTaskCreateDTO.getIcon())
                        .submitFields(baseTaskCreateDTO.getSubmitFields())
                        .webhookUrl(baseTaskCreateDTO.getWebhookUrl())
                        .webhookSecret(baseTaskCreateDTO.getWebhookSecret())
                        .build()
        );
    }

    @Operation(
            summary = "编辑原子任务",
            description = "整体更新任务定义；简介和图标省略或传 null 表示清空，submitFields 必填，传空数组清空提交字段。webhook 地址为空则清空地址和密钥，密钥留空则保持原密钥。关联模板和后续新建工作流使用新定义，已有工作流保留名称、字段和 webhook 快照，图标同步更新。启用/禁用状态不变"
    )
    @PutMapping("/{taskId}")
    @ResponseMessage("修改成功")
    public void updateBaseTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                               @PathVariable Long taskId,
                               @RequestBody @Valid BaseTaskCreateDTO dto) {
        baseTaskService.updateBaseTask(BaseTaskUpdateParamsBO.builder()
                .taskId(taskId)
                .orgId(sessionContext.orgId())
                .name(dto.getName())
                .description(dto.getDescription())
                .icon(dto.getIcon())
                .submitFields(dto.getSubmitFields())
                .webhookUrl(dto.getWebhookUrl())
                .webhookSecret(dto.getWebhookSecret())
                .build());
    }

    @Operation(
            summary = "启用/禁用原子任务",
            description = "禁用后该任务无法被添加到新模板节点中，已引用的节点不受影响。status: ENABLED-启用，DISABLED-禁用"
    )
    @PatchMapping("/{taskId}/status")
    @ResponseMessage("状态修改成功")
    public void changeBaseTaskStatus(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                              @PathVariable Long taskId,
                                              @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        baseTaskService.changeBaseTaskStatus(
                BaseTaskChangeStatusParamsBO.builder()
                        .taskId(taskId)
                        .orgId(sessionContext.orgId())
                        .status(changeStatusDTO.getStatus())
                        .build()
        );
    }

    @Operation(summary = "删除原子任务", description = "仅允许删除未被任务模板节点或项目任务节点引用的任务；已引用的任务应改为禁用")
    @DeleteMapping("/{taskId}")
    @ResponseMessage("删除成功")
    public void deleteBaseTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                               @PathVariable Long taskId) {
        baseTaskService.deleteBaseTask(BaseTaskDeleteParamsBO.builder()
                .taskId(taskId)
                .orgId(sessionContext.orgId())
                .build());
    }
}
