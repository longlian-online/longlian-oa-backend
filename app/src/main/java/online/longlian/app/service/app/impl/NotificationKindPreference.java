package online.longlian.app.service.app.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import online.longlian.common.enumeration.InboxNotificationKind;

import java.util.EnumSet;
import java.util.Set;

final class NotificationKindPreference {
    private NotificationKindPreference() {
    }

    static Set<InboxNotificationKind> disabled(String config) {
        if (config == null || config.isBlank()) {
            return Set.of();
        }
        JSONObject root = JSON.parseObject(config);
        if (root == null) {
            return Set.of();
        }
        Object raw = root.get("notificationKinds");
        if (!(raw instanceof JSONObject kinds)) {
            return Set.of();
        }
        EnumSet<InboxNotificationKind> disabled = EnumSet.noneOf(InboxNotificationKind.class);
        for (InboxNotificationKind kind : InboxNotificationKind.values()) {
            if (Boolean.FALSE.equals(kinds.getBoolean(kind.name()))) {
                disabled.add(kind);
            }
        }
        return disabled.isEmpty() ? Set.of() : Set.copyOf(disabled);
    }
}
