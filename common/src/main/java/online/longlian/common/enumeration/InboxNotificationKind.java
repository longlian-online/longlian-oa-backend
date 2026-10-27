package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "user_notification", field = "kind")
public enum InboxNotificationKind implements CodeEnum {
    TASK_READY(1, "可以开始"),
    ITEM_COMPLETED(2, "项目完成"),
    TASK_ASSIGNED(3, "任务分配"),
    JOIN_APPLICATION(4, "入组申请"),
    DELETION_REQUEST(5, "删除申请");

    private final Integer code;
    private final String desc;
}
