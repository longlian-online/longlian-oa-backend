package online.longlian.app.service.app.impl.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.app.*;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.service.resource.ResourceService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.longlian.app.mapper.UserNotificationMapper;
import online.longlian.app.pojo.bo.app.NotificationBatchCreateParamsBO;
import online.longlian.app.pojo.entity.UserNotification;
import online.longlian.app.service.app.InboxNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class InboxNotificationServiceImpl extends ServiceImpl<UserNotificationMapper, UserNotification>
        implements InboxNotificationService {
    private final NotificationSourceResolver sourceResolver;
    private final ResourceService resourceService;
    private final Clock clock;
    private final InboxEmailOutboxWriter inboxEmailOutboxWriter;

    @Override
    public NotificationStatusResultBO getStatus(NotificationStatusParamsBO params) {
        UserNotification unread = baseMapper.selectOne(inbox(params.getOrgId(), params.getUserId())
                .select(UserNotification::getId).isNull(UserNotification::getReadAt).last("LIMIT 1"));
        return new NotificationStatusResultBO(unread != null);
    }

    @Override
    public NotificationPageResultBO list(NotificationListParamsBO params) {
        LambdaQueryWrapper<UserNotification> query = inbox(params.getOrgId(), params.getUserId());
        if (params.getBeforeCreatedAt() != null) {
            query.and(cursor -> cursor.lt(UserNotification::getCreatedAt, params.getBeforeCreatedAt())
                    .or(sameTime -> sameTime.eq(UserNotification::getCreatedAt, params.getBeforeCreatedAt())
                            .lt(UserNotification::getId, params.getBeforeId())));
        }
        List<UserNotification> rows = baseMapper.selectList(query.orderByDesc(UserNotification::getCreatedAt)
                .orderByDesc(UserNotification::getId).last("LIMIT 21"));
        boolean hasNext = rows.size() > 20;
        List<UserNotification> page = hasNext ? rows.subList(0, 20) : rows;
        long unreadCount = baseMapper.selectCount(inbox(params.getOrgId(), params.getUserId())
                .isNull(UserNotification::getReadAt));
        Map<Long, NotificationResolvedSourceBO> sources = sourceResolver.resolve(new NotificationSourceResolveParamsBO(
                params.getOrgId(), page.stream().map(row -> new NotificationSourceRefBO(
                        row.getId(), row.getKind(), row.getSourceId())).toList()));
        List<Long> imageIds = sources.values().stream().flatMap(source -> source.getImageResourceIds().stream())
                .distinct().toList();
        Map<Long, ResourceReadUrlGetResultBO> images = imageIds.isEmpty() ? Map.of()
                : resourceService.getResourceReadUrls(imageIds);
        List<NotificationResultBO> list = page.stream().map(row -> {
            NotificationResolvedSourceBO source = sources.get(row.getId());
            String thumbnail = source.getImageResourceIds().stream().map(images::get)
                    .filter(image -> image != null && params.getOrgId().equals(image.getOrganizationId()))
                    .map(ResourceReadUrlGetResultBO::getUrl).filter(url -> url != null && !url.isBlank())
                    .findFirst().orElse(null);
            return new NotificationResultBO(row.getId(), row.getKind(), row.getSourceId(), row.getCreatedAt(),
                    row.getReadAt(), source.getSource(), thumbnail == null ? null
                    : thumbnail + (thumbnail.contains("?") ? "&" : "?") + "eo-img.resize=w/144/h/144");
        }).toList();
        return new NotificationPageResultBO(list, hasNext, unreadCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationReadResultBO markRead(NotificationReadParamsBO params) {
        UserNotification row = baseMapper.selectOne(inbox(params.getOrgId(), params.getUserId())
                .eq(UserNotification::getId, params.getId()).last("FOR UPDATE"));
        if (row == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "通知不存在");
        }
        if (row.getReadAt() != null) {
            return new NotificationReadResultBO(row.getId(), row.getReadAt());
        }
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        baseMapper.update(null, new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getId, row.getId()).set(UserNotification::getReadAt, now)
                .set(UserNotification::getUpdatedAt, now));
        return new NotificationReadResultBO(row.getId(), now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationReadAllResultBO markAllRead(NotificationReadAllParamsBO params) {
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        baseMapper.update(null, new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getOrgId, params.getOrgId())
                .eq(UserNotification::getRecipientUserId, params.getUserId())
                .isNull(UserNotification::getReadAt).set(UserNotification::getReadAt, now)
                .set(UserNotification::getUpdatedAt, now));
        return new NotificationReadAllResultBO(now);
    }

    private LambdaQueryWrapper<UserNotification> inbox(Long orgId, Long userId) {
        return new LambdaQueryWrapper<UserNotification>().eq(UserNotification::getOrgId, orgId)
                .eq(UserNotification::getRecipientUserId, userId);
    }
    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void createBatch(NotificationBatchCreateParamsBO params) {
        if (params.getNotifications().isEmpty()) {
            return;
        }
        List<UserNotification> notifications = params.getNotifications().stream().map(notification -> UserNotification.builder()
                .orgId(params.getOrgId())
                .recipientUserId(notification.getRecipientUserId())
                .kind(notification.getKind())
                .sourceId(notification.getSourceId())
                .triggerId(notification.getTriggerId())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getCreatedAt())
                .build()).toList();
        saveBatch(notifications);
        if (notifications.stream().anyMatch(notification -> notification.getId() == null)) {
            throw new IllegalStateException("站内通知未生成ID");
        }
        inboxEmailOutboxWriter.enqueue(params.getOrgId(), notifications);
    }
}
