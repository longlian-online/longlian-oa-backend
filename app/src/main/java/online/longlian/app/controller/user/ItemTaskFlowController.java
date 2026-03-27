package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.ItemTaskFlowCreateDTO;
import online.longlian.app.pojo.dto.ItemTaskNodeAddDTO;
import online.longlian.app.pojo.vo.ItemTaskFlowVO;
import online.longlian.app.pojo.vo.TaskTemplateDetailVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "项目任务流接口", description = "企划项目的任务流管理：创建、查询、节点增删")
@RequestMapping("/app/item")
@RestController
@RequiredArgsConstructor
public class ItemTaskFlowController {

    // private final ItemTaskFlowService itemTaskFlowService;

    // -------------------------
    // 任务流
    // -------------------------

    @Operation(
        summary = "为项目创建任务流",
        description = "基于指定模板快照节点结构创建任务流；创建后模板变更不影响该任务流节点"
    )
    @Parameter(name = "itemId", description = "项目ID")
    @PostMapping("/{itemId}/flow")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> createItemTaskFlow(
            @PathVariable Long itemId,
            @RequestBody @Valid ItemTaskFlowCreateDTO itemTaskFlowCreateDTO) {
        // TODO
        // return itemTaskFlowService.createItemTaskFlow(itemId, itemTaskFlowCreateDTO);
        return Result.success("创建成功");
    }

    @Operation(
        summary = "获取项目任务流（含节点执行状态）",
        description = "返回任务流基本信息及各节点当前执行状态，" +
                      "节点 taskStatus 为 null 表示前序节点未完成、当前节点尚未解锁"
    )
    @Parameter(name = "itemId", description = "项目ID")
    @GetMapping("/{itemId}/flow")
    public Result<ItemTaskFlowVO> getItemTaskFlow(@PathVariable Long itemId) {
        // TODO
        // return itemTaskFlowService.getItemTaskFlow(itemId);
        return Result.success("查询成功", null);
    }

}
