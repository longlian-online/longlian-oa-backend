package online.longlian.app.common.event;

import lombok.Data;
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
@Data
public class InboxMessageEvent extends ApplicationEvent {

    /** 接收方类型：USER（个人）/ ORGANIZATION（组织） */
    private final InboxTargetType targetType;

    /** 接收方 ID：userId（个人）或 orgId（组织） */
    private final Long targetId;

    /** 消息内容 */
    private final InboxMessageBO messageBO;
}
