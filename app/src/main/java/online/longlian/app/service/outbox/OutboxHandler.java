package online.longlian.app.service.outbox;

import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.common.enumeration.OutboxMessageType;

import java.time.LocalDateTime;

public interface OutboxHandler {
    OutboxMessageType type();

    void deliver(OutboxMessage message, LocalDateTime executeTime);
}
