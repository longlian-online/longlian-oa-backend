package online.longlian.app.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.enumeration.InboxTargetType;
import online.longlian.app.service.inbox.InboxMessageService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboxMessageEventListener {

    private final InboxMessageService inboxMessageService;

    @Async
    @EventListener
    public void handleInboxMessageEvent(InboxMessageEvent event) {
        try {
            InboxTargetType targetType = event.getTargetType();
            Long targetId = event.getTargetId();
            if (targetType == InboxTargetType.USER) {
                inboxMessageService.sendToUser(targetId, event.getMessageDTO());
            } else if (targetType == InboxTargetType.ORGANIZATION) {
                inboxMessageService.sendToOrganization(targetId, event.getMessageDTO());
            }
        } catch (Exception e) {
            log.error("处理站内消息事件失败，targetType={}, targetId={}", event.getTargetType(), event.getTargetId(), e);
        }
    }
}
