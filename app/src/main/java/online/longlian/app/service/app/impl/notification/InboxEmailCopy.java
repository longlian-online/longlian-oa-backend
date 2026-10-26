package online.longlian.app.service.app.impl.notification;

import online.longlian.app.pojo.bo.app.NotificationSourceResultBO;
import online.longlian.app.pojo.bo.app.NotificationTaskSlotBO;
import online.longlian.common.enumeration.InboxNotificationKind;

final class InboxEmailCopy {
    private InboxEmailCopy() {
    }

    static String message(InboxNotificationKind kind, NotificationSourceResultBO source) {
        String title = "「" + source.getProjectTitle() + " · " + source.getItemTitle() + "」";
        return switch (kind) {
            case TASK_READY -> title + "的前置步骤已完成，你接取的「" + slotLabel(source.getTaskSlot()) + "」可以开始了。";
            case TASK_ASSIGNED -> title + "的「" + slotLabel(source.getTaskSlot()) + "」已分配给你，无需手动领取。";
            case ITEM_COMPLETED -> title + "的全部任务已完成。";
        };
    }

    static String slotLabel(NotificationTaskSlotBO slot) {
        String named = slot.getSerialStep() != null && slot.getSerialStep() > 0
                ? slot.getName() + "（第 " + slot.getSerialStep() + " 步）"
                : slot.getName();
        if (slot.getSlotTotal() > 1) {
            return named + " · " + slot.getSlotIndex() + "/" + slot.getSlotTotal();
        }
        return named;
    }
}
