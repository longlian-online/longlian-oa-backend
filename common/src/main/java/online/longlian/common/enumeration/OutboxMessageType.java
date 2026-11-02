package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "outbox_message", field = "type")
public enum OutboxMessageType implements CodeEnum {
    INBOX_EMAIL(1, "站内通知邮件"),
    TASK_WEBHOOK(2, "任务 webhook");

    private final Integer code;
    private final String desc;
}
