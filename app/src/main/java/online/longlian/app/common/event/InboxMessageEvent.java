package online.longlian.app.common.event;

import lombok.Getter;
import online.longlian.app.common.enumeration.InboxTargetType;
import online.longlian.app.pojo.bo.InboxMessageBO;
import org.springframework.context.ApplicationEvent;

/**
 * 站内信事件，用于异步发送站内信。
 *
 * <p>业务方在需要发送站内信时 publish 此事件即可，无需直接依赖 InboxMessageService。
 * 由 {@link InboxMessageEventListener} 异步消费，不阻塞主流程。</p>
 *
 * <p>使用示例：
 * <pre>{@code
 * eventPublisher.publishEvent(new InboxMessageEvent(
 *         this, InboxTargetType.USER, userId, bo));
 * }</pre>
 *
 * @see InboxMessageEventListener
 * @see InboxTargetType
 * @see InboxMessageBO
 */
@Getter
public class InboxMessageEvent extends ApplicationEvent {

    private final InboxTargetType targetType;
    private final Long targetId;
    private final InboxMessageBO messageBO;

    public InboxMessageEvent(Object source, InboxTargetType targetType, Long targetId, InboxMessageBO messageBO) {
        super(source);
        this.targetType = targetType;
        this.targetId = targetId;
        this.messageBO = messageBO;
    }
}
