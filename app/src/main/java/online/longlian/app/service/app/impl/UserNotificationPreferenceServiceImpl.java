package online.longlian.app.service.app.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.app.UserNotificationPreferenceService;
import online.longlian.common.enumeration.InboxNotificationKind;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserNotificationPreferenceServiceImpl implements UserNotificationPreferenceService {
    private final UserMapper userMapper;

    @Override
    public Map<Long, Set<InboxNotificationKind>> disabledKinds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = userIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<InboxNotificationKind>> disabled = new LinkedHashMap<>();
        for (User user : userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getConfig)
                .in(User::getId, ids))) {
            Set<InboxNotificationKind> kinds = NotificationKindPreference.disabled(user.getConfig());
            if (!kinds.isEmpty()) {
                disabled.put(user.getId(), kinds);
            }
        }
        return disabled;
    }
}
