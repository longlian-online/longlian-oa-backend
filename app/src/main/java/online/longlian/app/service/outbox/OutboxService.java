package online.longlian.app.service.outbox;

import online.longlian.app.pojo.entity.OutboxMessage;

import java.util.List;

public interface OutboxService {
    void enqueueAll(List<OutboxMessage> messages);
}
