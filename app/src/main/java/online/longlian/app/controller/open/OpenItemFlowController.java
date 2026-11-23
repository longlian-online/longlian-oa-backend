package online.longlian.app.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.pojo.bo.app.itemtaskflow.ItemTaskFlowResultBO;
import online.longlian.app.pojo.bo.app.itemtaskflow.ItemTaskNodeResultBO;
import online.longlian.app.pojo.bo.common.TaskFormFieldBO;
import online.longlian.app.pojo.vo.open.OpenItemFlowNodeVO;
import online.longlian.app.pojo.vo.open.OpenItemFlowVO;
import online.longlian.app.pojo.vo.open.OpenSubmitFieldVO;
import online.longlian.app.service.app.itemtaskflow.ItemTaskFlowService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放面项目任务流查询。组织上下文全部来自密钥解出的 principal，不接受请求指定。
 */
@RequiredArgsConstructor
@Tag(name = "开放接口-任务流", description = "密钥认证的项目任务流查询")
@SecurityRequirement(name = "XApiKey")
@RequestMapping("/open/v1/items")
@RestController
public class OpenItemFlowController {

    private final ItemTaskFlowService itemTaskFlowService;

    @Operation(summary = "项目任务流",
            description = "自动化核心：节点 submitFields 是提交表单快照，提交时按 key 组装 values；"
                    + "taskStatus 为 null 表示前序节点未完成、当前节点尚未解锁")
    @GetMapping("/{itemId}/flow")
    public OpenItemFlowVO getItemTaskFlow(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @PathVariable Long itemId) {
        ItemTaskFlowResultBO bo = itemTaskFlowService.getItemTaskFlow(itemId, principal.orgId());
        OpenItemFlowVO vo = new OpenItemFlowVO();
        vo.setItemTitle(bo.getItemTitle());
        vo.setStatus(bo.getStatus() == null ? null : bo.getStatus().getDesc());
        vo.setCompletedAt(bo.getCompletedAt());
        vo.setHasSubmissionRecord(bo.getHasSubmissionRecord());
        vo.setNodes(bo.getNodes() == null ? List.of() : bo.getNodes().stream().map(this::toNodeVO).toList());
        return vo;
    }

    private OpenItemFlowNodeVO toNodeVO(ItemTaskNodeResultBO node) {
        OpenItemFlowNodeVO vo = new OpenItemFlowNodeVO();
        vo.setBaseTaskId(node.getBaseTaskId());
        vo.setName(node.getName());
        vo.setSubmitFields(node.getSubmitFields() == null ? List.of()
                : node.getSubmitFields().stream().map(this::toFieldVO).toList());
        vo.setSort(node.getSort());
        vo.setParallelSort(node.getParallelSort());
        vo.setTaskInstanceId(node.getTaskInstanceId());
        vo.setTaskStatus(node.getTaskStatus());
        vo.setRevision(node.getRevision());
        return vo;
    }

    private OpenSubmitFieldVO toFieldVO(TaskFormFieldBO field) {
        return new OpenSubmitFieldVO(field.key(), field.label(), field.type(), field.required(), field.options());
    }
}
