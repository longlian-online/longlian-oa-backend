package online.longlian.app.service.app.impl;

import online.longlian.common.enumeration.InboxNotificationKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationKindPreferenceTest {
    @Test
    void shouldTreatOnlyExplicitFalseAsDisabled() {
        assertThat(NotificationKindPreference.disabled(null)).isEmpty();
        assertThat(NotificationKindPreference.disabled(" ")).isEmpty();
        assertThat(NotificationKindPreference.disabled("{}")).isEmpty();
        assertThat(NotificationKindPreference.disabled("{\"notificationKinds\":[]}")).isEmpty();
        assertThat(NotificationKindPreference.disabled(
                "{\"emailNotificationsEnabled\":false,\"notificationKinds\":{\"TASK_READY\":false,\"ITEM_COMPLETED\":true,\"UNKNOWN\":false}}"))
                .containsExactly(InboxNotificationKind.TASK_READY);
    }
}
