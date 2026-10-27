package online.longlian.app.service.app.impl.notification;

import online.longlian.app.pojo.bo.app.NotificationSourceResultBO;
import online.longlian.app.pojo.bo.app.NotificationTaskSlotBO;
import online.longlian.common.enumeration.InboxNotificationKind;

final class InboxEmailCopy {
    private InboxEmailCopy() {
    }

    static String message(InboxNotificationKind kind, NotificationSourceResultBO source) {
        return switch (kind) {
            case JOIN_APPLICATION -> "「" + source.getApplicantName() + "」申请加入组织。";
            case DELETION_REQUEST -> "「" + source.getRequesterName() + "」申请删除"
                    + deletionTarget(source.getTargetType()) + "「" + source.getTargetTitle() + "」。";
            case TASK_READY -> taskTitle(source) + "的前置步骤已完成，你接取的「" + slotLabel(source.getTaskSlot()) + "」可以开始了。";
            case TASK_ASSIGNED -> taskTitle(source) + "的「" + slotLabel(source.getTaskSlot()) + "」已分配给你，无需手动领取。";
            case ITEM_COMPLETED -> taskTitle(source) + "的全部任务已完成。";
        };
    }

    private static String taskTitle(NotificationSourceResultBO source) {
        return "「" + source.getProjectTitle() + " · " + source.getItemTitle() + "」";
    }

    private static String deletionTarget(String targetType) {
        if ("PROJECT".equals(targetType)) {
            return "企划";
        }
        if ("ITEM".equals(targetType)) {
            return "项目";
        }
        throw new IllegalStateException("来源不可用");
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
