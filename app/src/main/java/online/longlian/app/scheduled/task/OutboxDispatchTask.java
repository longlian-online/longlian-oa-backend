package online.longlian.app.scheduled.task;

import online.longlian.app.pojo.bo.common.ScheduledTaskDefinition;
import online.longlian.app.scheduled.ScheduledTask;
import online.longlian.app.service.outbox.OutboxDispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OutboxDispatchTask implements ScheduledTask {
    private static final ScheduledTaskDefinition DEFINITION = new ScheduledTaskDefinition(
            "outbox-dispatch", "投递到期的 outbox 消息", "0/15 * * * * ?", true);

    private final OutboxDispatcher dispatcher;
    private final boolean dispatchEnabled;

    public OutboxDispatchTask(OutboxDispatcher dispatcher,
                              @Value("${outbox.dispatch-enabled:true}") boolean dispatchEnabled) {
        this.dispatcher = dispatcher;
        this.dispatchEnabled = dispatchEnabled;
    }

    @Override
    public ScheduledTaskDefinition getDefinition() {
        return DEFINITION;
    }

    @Override
    public void execute(LocalDateTime executeTime) {
        if (!dispatchEnabled) {
            return;
        }
        dispatcher.dispatch(executeTime);
    }
}
