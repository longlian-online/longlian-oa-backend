package online.longlian.app.common.event;

import lombok.Getter;
import online.longlian.app.common.enumeration.InboxTargetType;
import online.longlian.app.pojo.dto.common.InboxMessageDTO;
import org.springframework.context.ApplicationEvent;

@Getter
public class InboxMessageEvent extends ApplicationEvent {

    private final InboxTargetType targetType;
    private final Long targetId;
    private final InboxMessageDTO messageDTO;

    public InboxMessageEvent(Object source, InboxTargetType targetType, Long targetId, InboxMessageDTO messageDTO) {
        super(source);
        this.targetType = targetType;
        this.targetId = targetId;
        this.messageDTO = messageDTO;
    }
}
