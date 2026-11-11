package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.itemtaskflow.ItemTaskFlowResultBO;
import online.longlian.app.pojo.bo.app.itemtaskflow.ItemTaskFlowUpdateParamsBO;
import online.longlian.app.pojo.dto.app.ItemTaskFlowRemovedNodeDTO;
import online.longlian.app.pojo.dto.app.ItemTaskFlowUpdateDTO;
import online.longlian.app.pojo.vo.app.ItemTaskFlowVO;
import online.longlian.app.pojo.vo.app.ItemTaskNodeVO;
import online.longlian.app.service.app.ItemTaskFlowService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "项目任务流接口", description = "项目任务流查询")
@RequestMapping("/app/item")
@RestController
@RequiredArgsConstructor
public class ItemTaskFlowController {

    private final ItemTaskFlowService itemTaskFlowService;

    @Operation(
        summary = "获取项目任务流（含节点执行状态）",
        description = "返回任务流基本信息及各节点当前执行状态，" +
                      "节点 taskStatus 为 null 表示前序节点未完成、当前节点尚未解锁"
    )
    @GetMapping("/{itemId}/flow")
    @ResponseMessage("查询成功")
    public ItemTaskFlowVO getItemTaskFlow(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                           @PathVariable Long itemId) {
        ItemTaskFlowResultBO resultBO = itemTaskFlowService.getItemTaskFlow(itemId, sessionContext.orgId());

        ItemTaskFlowVO itemTaskFlowVO = new ItemTaskFlowVO();
        BeanUtils.copyProperties(resultBO, itemTaskFlowVO, "nodes");
        itemTaskFlowVO.setNodes(resultBO.getNodes().stream().map(nodeBO -> {
            ItemTaskNodeVO nodeVO = new ItemTaskNodeVO();
            BeanUtils.copyProperties(nodeBO, nodeVO);
            return nodeVO;
        }).toList());
        return itemTaskFlowVO;
    }

    @Operation(
            summary = "保存项目工作流",
            description = "仅企划负责人可操作。可添加启用中的原子任务、删除尚未接取且没有提交记录的节点，并调整顺序。"
                    + "已有节点的修订号必须与当前任务一致，否则整次拒绝。"
    )
    @PutMapping("/{itemId}/flow")
    @ResponseMessage("保存成功")
    public void updateItemTaskFlow(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                    @PathVariable Long itemId,
                                    @RequestBody @Valid ItemTaskFlowUpdateDTO dto) {
        itemTaskFlowService.updateItemTaskFlow(ItemTaskFlowUpdateParamsBO.builder()
                .itemId(itemId)
                .orgId(sessionContext.orgId())
                .operatorId(sessionContext.userId())
                .nodes(dto.getNodes().stream().map(node -> ItemTaskFlowUpdateParamsBO.TaskFlowNodeParamsBO.builder()
                        .id(node.getId())
                        .baseTaskId(node.getBaseTaskId())
                        .sort(node.getSort())
                        .parallelSort(node.getParallelSort())
                        .name(node.getName())
                        .revision(parseRevision(node.getRevision(), node.getId() != null))
                        .build()).toList())
                .removed(dto.getRemoved() == null ? List.of() : dto.getRemoved().stream()
                        .map(this::toRemoved)
                        .toList())
                .build());
    }

    private ItemTaskFlowUpdateParamsBO.TaskFlowNodeDeleteParamsBO toRemoved(ItemTaskFlowRemovedNodeDTO node) {
        return ItemTaskFlowUpdateParamsBO.TaskFlowNodeDeleteParamsBO.builder()
                .id(node.getId())
                .revision(parseRevision(node.getRevision(), true))
                .build();
    }

    private Long parseRevision(String revision, boolean required) {
        if (revision == null || revision.isBlank()) {
            if (required) {
                throw new AppException(ResultCode.PARAM_ERROR, "任务修订号不能为空");
            }
            return null;
        }
        try {
            return Long.valueOf(revision);
        } catch (NumberFormatException exception) {
            throw new AppException(ResultCode.PARAM_ERROR, "任务修订号无效");
        }
    }
}
