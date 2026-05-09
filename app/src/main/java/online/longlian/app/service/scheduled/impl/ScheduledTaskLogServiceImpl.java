package online.longlian.app.service.scheduled.impl;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ScheduledTaskLogMapper;
import online.longlian.app.pojo.entity.ScheduledTaskLog;
import online.longlian.app.service.scheduled.ScheduledTaskLogService;
import online.longlian.common.enumeration.ScheduledTaskStatus;
import online.longlian.common.enumeration.TriggerSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScheduledTaskLogServiceImpl implements ScheduledTaskLogService {

    private final ScheduledTaskLogMapper taskLogMapper;
    private final IdentifierGenerator idGenerator = new DefaultIdentifierGenerator();

    @Override
    public Long insertRunningLog(String taskName, LocalDateTime executeTime,
                                 TriggerSource source, String traceId, Long triggeredBy, LocalDateTime startedAt) {
        Long logId = idGenerator.nextId(new ScheduledTaskLog()).longValue();
        ScheduledTaskLog logEntry = ScheduledTaskLog.builder()
                .id(logId)
                .taskName(taskName)
                .triggerSource(source)
                .executeTimeParam(executeTime)
                .status(ScheduledTaskStatus.RUNNING)
                .executionId(traceId)
                .triggeredBy(triggeredBy)
                .startedAt(startedAt)
                .build();
        taskLogMapper.insert(logEntry);
        return logId;
    }

    @Override
    public void updateLog(Long logId, ScheduledTaskStatus status, String errorMessage,
                          LocalDateTime endedAt, long durationMs) {
        ScheduledTaskLog update = ScheduledTaskLog.builder()
                .id(logId)
                .status(status)
                .errorMessage(errorMessage)
                .endedAt(endedAt)
                .durationMs(durationMs)
                .build();
        taskLogMapper.updateById(update);
    }
}