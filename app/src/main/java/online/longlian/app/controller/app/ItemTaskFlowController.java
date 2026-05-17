package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.vo.app.ItemTaskFlowVO;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "项目任务流接口", description = "项目任务流查询、任务实例列表")
@RequestMapping("/app/item")
@RestController
@RequiredArgsConstructor
public class ItemTaskFlowController {

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

    @Operation(
            summary = "查询项目下的任务实例列表",
            description = "返回当前项目所有已生成的任务实例"
    )
    @Parameter(name = "itemId", description = "项目ID")
    @GetMapping("/{itemId}/task-instances")
    public Result<List<ItemTaskInstanceVO>> listItemTaskInstances(@PathVariable Long itemId) {
        // TODO
        // return itemTaskFlowService.listItemTaskInstances(itemId);
        return Result.success("查询成功", null);
    }
}
