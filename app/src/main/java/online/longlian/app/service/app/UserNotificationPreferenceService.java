package online.longlian.app.service.app;

import online.longlian.common.enumeration.InboxNotificationKind;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface UserNotificationPreferenceService {
    /**
     * 返回显式关闭了站内通知类型的用户。未出现的用户和类型都表示仍接收。
     */
    Map<Long, Set<InboxNotificationKind>> disabledKinds(Collection<Long> userIds);
}
