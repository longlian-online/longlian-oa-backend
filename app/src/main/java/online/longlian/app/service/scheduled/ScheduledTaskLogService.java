package online.longlian.app.service.scheduled;

import online.longlian.app.pojo.entity.ScheduledTaskLog;
import online.longlian.common.enumeration.ScheduledTaskStatus;
import online.longlian.common.enumeration.TriggerSource;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledTaskLogService {

    /**
     * 记录任务开始执行，返回生成的日志ID
     */
    Long insertRunningLog(String taskName, LocalDateTime executeTime,
                          TriggerSource source, String traceId, Long triggeredBy, LocalDateTime startedAt);

    /**
     * 更新任务执行结果
     */
    void updateLog(Long logId, ScheduledTaskStatus status, String errorMessage,
                   LocalDateTime endedAt, long durationMs);
}