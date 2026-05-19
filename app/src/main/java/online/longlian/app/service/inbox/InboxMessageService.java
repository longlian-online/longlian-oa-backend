package online.longlian.app.service.inbox;

import online.longlian.app.pojo.dto.common.InboxMessageDTO;
import online.longlian.app.pojo.vo.app.InboxMessageVO;
import online.longlian.app.pojo.vo.common.PageResultVO;

public interface InboxMessageService {

    void sendToUser(Long userId, InboxMessageDTO dto);

    void sendToOrganization(Long orgId, InboxMessageDTO dto);

    void markAsRead(Long messageId, Long userId);

    PageResultVO<InboxMessageVO> getPage(Long userId, Integer page, Integer size);
}
