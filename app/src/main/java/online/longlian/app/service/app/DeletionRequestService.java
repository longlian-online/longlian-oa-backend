package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.DeletionRequestInfoBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.common.enumeration.DeletionRequestStatus;

public interface DeletionRequestService {

    void createProjectRequest(Long projectId, Long userId, Long orgId, String reason);

    void createItemRequest(Long projectId, Long itemId, Long userId, Long orgId, String reason);

    PageResultBO<DeletionRequestInfoBO> listPending(Long orgId, int pageNum, int pageSize);

    void review(Long requestId, Long orgId, Long reviewerId, DeletionRequestStatus status);
}
