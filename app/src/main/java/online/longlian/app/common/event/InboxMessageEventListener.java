package online.longlian.app.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.enumeration.InboxTargetType;
import online.longlian.app.service.inbox.InboxMessageService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 站内信事件监听器，异步消费 {@link InboxMessageEvent}。
 *
 * <p>根据事件中的 {@link InboxTargetType} 路由到不同发送方式：
 * <ul>
 *   <li>{@link InboxTargetType#USER} → {@link InboxMessageService#sendToUser}</li>
 *   <li>{@link InboxTargetType#ORGANIZATION} → {@link InboxMessageService#sendToOrganization}</li>
 * </ul>
 *
 * <p>消费失败只打日志，不影响主事务。
 *
 * @see InboxMessageEvent
 * @see InboxMessageService
 */
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
                inboxMessageService.sendToUser(targetId, event.getMessageBO());
            } else if (targetType == InboxTargetType.ORGANIZATION) {
                inboxMessageService.sendToOrganization(targetId, event.getMessageBO());
            }
        } catch (Exception e) {
            log.error("处理站内消息事件失败，targetType={}, targetId={}", event.getTargetType(), event.getTargetId(), e);
        }
    }
}
