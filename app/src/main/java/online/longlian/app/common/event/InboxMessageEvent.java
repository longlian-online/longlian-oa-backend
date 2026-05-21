package online.longlian.app.common.event;

import lombok.Getter;
import online.longlian.app.common.enumeration.InboxTargetType;
import online.longlian.app.pojo.bo.InboxMessageBO;
import org.springframework.context.ApplicationEvent;

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
