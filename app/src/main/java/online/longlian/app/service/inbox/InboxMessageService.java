package online.longlian.app.service.inbox;

import online.longlian.app.pojo.bo.InboxMessageBO;
import online.longlian.app.pojo.vo.app.InboxMessageVO;
import online.longlian.app.pojo.vo.common.PageResultVO;

/**
 * 站内信模块
 *
 * <p>支持个人消息和组织消息两种发送方式：
 * <ul>
 *   <li><b>个人消息</b> — {@link #sendToUser(Long, InboxMessageBO)}，指定接收人 userId，消息仅该用户可见</li>
 *   <li><b>组织消息</b> — {@link #sendToOrganization(Long, InboxMessageBO)}，指定 orgId，消息对组织内所有成员可见</li>
 * </ul>
 *
 * <p><b>事件驱动方式：</b>
 * 如果不想直接依赖 {@code InboxMessageService}，可以 publish {@link online.longlian.app.common.event.InboxMessageEvent}，
 * 由 {@link online.longlian.app.common.event.InboxMessageEventListener} 异步消费，与主流程解耦。
 * <pre>{@code
 * eventPublisher.publishEvent(new InboxMessageEvent(
 *         this, InboxTargetType.USER, userId, bo));
 * }</pre>
 */
public interface InboxMessageService {

    void sendToUser(Long userId, InboxMessageBO bo);

    void sendToOrganization(Long orgId, InboxMessageBO bo);

    void markAsRead(Long messageId, Long userId);

    PageResultVO<InboxMessageVO> getPage(Long userId, Integer page, Integer size);
}
