package online.longlian.app.service.inbox;

import online.longlian.app.pojo.bo.InboxMessageBO;
import online.longlian.app.pojo.vo.app.InboxMessageVO;
import online.longlian.app.pojo.vo.common.PageResultVO;

public interface InboxMessageService {

    void sendToUser(Long userId, InboxMessageBO bo);

    void sendToOrganization(Long orgId, InboxMessageBO bo);

    void markAsRead(Long messageId, Long userId);

    PageResultVO<InboxMessageVO> getPage(Long userId, Integer page, Integer size);
}
