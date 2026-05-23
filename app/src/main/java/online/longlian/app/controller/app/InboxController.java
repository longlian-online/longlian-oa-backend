package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.vo.app.InboxMessageVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.inbox.InboxMessageService;
import online.longlian.app.service.user.SessionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "站内信接口", description = "站内消息相关接口")
@RequestMapping("/app/inbox")
@RequiredArgsConstructor
@RestController
@Validated
public class InboxController {

    private final InboxMessageService inboxMessageService;
    private final SessionService sessionService;

    @Operation(summary = "分页查询站内消息列表")
    @GetMapping("/messages")
    public Result<PageResultVO<InboxMessageVO>> getMessages(
            @Parameter(description = "页码") @RequestParam @Min(1) @Validated Integer page,
            @Parameter(description = "每页数量") @RequestParam @Min(1) @Max(100) @Validated Integer size) {
        Long userId = sessionService.getCurrentUserId();
        return Result.success("查询成功", inboxMessageService.getPage(userId, page, size));
    }

    @Operation(summary = "标记消息为已读")
    @Parameter(name = "messageId", description = "消息ID")
    @PostMapping("/{messageId}/read")
    public Result<Void> markAsRead(@PathVariable Long messageId) {
        Long userId = sessionService.getCurrentUserId();
        inboxMessageService.markAsRead(messageId, userId);
        return Result.success("已标记为已读");
    }
}
