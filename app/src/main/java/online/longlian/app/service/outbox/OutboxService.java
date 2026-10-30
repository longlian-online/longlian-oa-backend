package online.longlian.app.service.outbox;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.common.enumeration.OutboxMessageStatus;
import online.longlian.common.enumeration.OutboxMessageType;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxService {
    void enqueueAll(List<OutboxMessage> messages);

    Page<OutboxMessage> listScoped(OutboxMessageType type, OutboxMessageStatus status, Long orgId,
                                   long pageNum, long pageSize);

    /** 只重放同一组织、同一类型里已经失败的消息。找不到时返回 false。 */
    boolean replayFailed(OutboxMessageType type, Long orgId, Long messageId, LocalDateTime now);
}
