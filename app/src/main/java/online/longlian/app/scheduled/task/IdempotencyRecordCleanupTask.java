package online.longlian.app.scheduled.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.mapper.IdempotencyRecordMapper;
import online.longlian.app.pojo.bo.common.ScheduledTaskDefinitionBO;
import online.longlian.app.pojo.entity.IdempotencyRecord;
import online.longlian.app.scheduled.ScheduledTask;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 幂等记录保留期清理：按创建时间物理删除 7 天前的记录，控制表体积。
 * 删除按保留期幂等，重复执行无副作用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyRecordCleanupTask implements ScheduledTask {

    private static final int RETENTION_DAYS = 7;

    private final IdempotencyRecordMapper idempotencyRecordMapper;

    @Override
    public ScheduledTaskDefinitionBO getDefinition() {
        return new ScheduledTaskDefinitionBO(
                "idempotency-record-cleanup",
                "清理超过 7 天保留期的开放接口幂等记录",
                "0 0 4 * * *",
                true
        );
    }

    @Override
    public void execute(LocalDateTime executeTime) {
        LocalDateTime expireBefore = executeTime.minusDays(RETENTION_DAYS);
        int deleted = idempotencyRecordMapper.delete(new LambdaQueryWrapper<IdempotencyRecord>()
                .lt(IdempotencyRecord::getCreatedAt, expireBefore));
        log.info("幂等记录保留期清理完成 | expireBefore={} | deleted={}", expireBefore, deleted);
    }
}
