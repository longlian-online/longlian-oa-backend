package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.InboxNotificationKind;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailNotificationRecipientResultBO {
    private String email;
    private boolean emailNotificationsEnabled;
    private Set<InboxNotificationKind> disabledKinds;

    public UserEmailNotificationRecipientResultBO(String email, boolean emailNotificationsEnabled) {
        this(email, emailNotificationsEnabled, Set.of());
    }

    public boolean rejectsKind(InboxNotificationKind kind) {
        return disabledKinds != null && disabledKinds.contains(kind);
    }
}
