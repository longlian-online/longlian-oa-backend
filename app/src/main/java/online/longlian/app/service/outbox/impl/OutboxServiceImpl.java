package online.longlian.app.service.outbox.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.longlian.app.mapper.OutboxMessageMapper;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.service.outbox.OutboxService;
import online.longlian.common.enumeration.OutboxMessageStatus;
import online.longlian.common.enumeration.OutboxMessageType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxServiceImpl extends ServiceImpl<OutboxMessageMapper, OutboxMessage> implements OutboxService {

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void enqueueAll(List<OutboxMessage> messages) {
        saveBatch(messages);
    }

    @Override
    public Page<OutboxMessage> listScoped(OutboxMessageType type, OutboxMessageStatus status, Long orgId,
                                          long pageNum, long pageSize) {
        return baseMapper.selectPage(new Page<>(pageNum, pageSize), scoped(type, status, orgId)
                .orderByDesc(OutboxMessage::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replayFailed(OutboxMessageType type, Long orgId, Long messageId, LocalDateTime now) {
        return baseMapper.update(null, new LambdaUpdateWrapper<OutboxMessage>()
                .set(OutboxMessage::getStatus, OutboxMessageStatus.PENDING)
                .set(OutboxMessage::getAttemptCount, 0)
                .set(OutboxMessage::getAvailableAt, now)
                .set(OutboxMessage::getLockedUntil, null)
                .set(OutboxMessage::getLastError, null)
                .set(OutboxMessage::getUpdatedAt, now)
                .eq(OutboxMessage::getId, messageId)
                .eq(OutboxMessage::getType, type)
                .eq(OutboxMessage::getStatus, OutboxMessageStatus.FAILED)
                .apply(orgPredicate(), orgId)) == 1;
    }

    private static LambdaQueryWrapper<OutboxMessage> scoped(OutboxMessageType type, OutboxMessageStatus status,
                                                            Long orgId) {
        return new LambdaQueryWrapper<OutboxMessage>()
                .eq(OutboxMessage::getType, type)
                .eq(OutboxMessage::getStatus, status)
                .apply(orgPredicate(), orgId);
    }

    /** payload.orgId 在写入时就是数字。按组织过滤，避免一个管理员重放另一个组织的失败记录。 */
    private static String orgPredicate() {
        return "CAST(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.orgId')) AS UNSIGNED) = {0}";
    }
}
