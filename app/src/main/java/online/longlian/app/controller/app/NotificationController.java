package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.*;
import online.longlian.app.pojo.dto.app.NotificationListDTO;
import online.longlian.app.pojo.vo.app.*;
import online.longlian.app.service.app.InboxNotificationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app/notifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "站内通知接口")
public class NotificationController {
    private final InboxNotificationService inboxNotificationService;

    @GetMapping("/status")
    @Operation(summary = "检查当前收件箱是否有未读通知")
    public NotificationStatusVO status(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext session) {
        var result = inboxNotificationService.getStatus(new NotificationStatusParamsBO(session.userId(), session.orgId()));
        return new NotificationStatusVO(result.isHasUnread());
    }

    @GetMapping
    @Operation(summary = "按游标读取最新 20 条通知及当前来源内容")
    public NotificationPageVO list(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext session,
                                    @Valid @ModelAttribute NotificationListDTO dto) {
        var result = inboxNotificationService.list(new NotificationListParamsBO(session.userId(), session.orgId(),
                dto.getBeforeCreatedAt(), dto.getBeforeId()));
        return new NotificationPageVO(result.getList().stream().map(this::toVO).toList(),
                result.isHasNext(), result.getUnreadCount());
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记当前收件箱的单条通知已读")
    public NotificationReadVO read(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext session,
                                    @PathVariable @Positive Long id) {
        var result = inboxNotificationService.markRead(new NotificationReadParamsBO(session.userId(), session.orgId(), id));
        return new NotificationReadVO(result.getId(), result.getReadAt());
    }

    @PutMapping("/read-all")
    @Operation(summary = "将当前组织完整收件箱全部标为已读")
    public NotificationReadAllVO readAll(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext session) {
        var result = inboxNotificationService.markAllRead(new NotificationReadAllParamsBO(session.userId(), session.orgId()));
        return new NotificationReadAllVO(result.getReadAt());
    }

    private NotificationVO toVO(NotificationResultBO notification) {
        var source = notification.getSource();
        NotificationSourceVO sourceVO = null;
        if (source != null) {
            var slot = source.getTaskSlot();
            sourceVO = new NotificationSourceVO(source.getProjectId(), source.getProjectTitle(), source.getItemId(),
                    source.getItemTitle(), slot == null ? null : new NotificationTaskSlotVO(slot.getName(),
                    slot.getSlotIndex(), slot.getSlotTotal(), slot.getSerialStep()));
        }
        return new NotificationVO(notification.getId(), notification.getKind().name(), notification.getSourceId(),
                notification.getCreatedAt(), notification.getReadAt(), sourceVO, notification.getThumbnailUrl());
    }
}
