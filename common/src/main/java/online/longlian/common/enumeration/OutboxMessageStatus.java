package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "outbox_message", field = "status")
public enum OutboxMessageStatus implements CodeEnum {
    PENDING(1, "待投递"),
    PROCESSING(2, "投递中"),
    SENT(3, "已发送"),
    FAILED(4, "失败"),
    SKIPPED(5, "已跳过");

    private final Integer code;
    private final String desc;
}
