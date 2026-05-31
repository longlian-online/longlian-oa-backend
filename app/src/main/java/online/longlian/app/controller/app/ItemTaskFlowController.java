package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.vo.app.ItemTaskFlowVO;
import online.longlian.app.service.app.ItemTaskFlowService;
import org.springframework.web.bind.annotation.*;

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
    public ItemTaskFlowVO getItemTaskFlow(@UserSession SessionContext sessionContext,
                                           @PathVariable Long itemId) {
        return itemTaskFlowService.getItemTaskFlow(itemId);
    }
}
