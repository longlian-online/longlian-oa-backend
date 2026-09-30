package online.longlian.app.scheduled.task;

import lombok.extern.slf4j.Slf4j;
import online.longlian.app.pojo.bo.common.ScheduledTaskDefinition;
import online.longlian.app.scheduled.ScheduledTask;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 心跳检测任务（示例）。
 * <p>
 * 纯粹用于验证定时任务链路是否正常工作，不涉及业务逻辑。
 * 上线后可删除或替换为真实业务任务。
 */
@Slf4j
@Component
public class HeartbeatTask implements ScheduledTask {
    private static final ScheduledTaskDefinition DEFINITION = new ScheduledTaskDefinition(
            "heartbeat", "心跳检测任务，每 5 分钟执行一次，用于验证调度链路", "0 0/5 * * * ?", false);

    @Override
    public ScheduledTaskDefinition getDefinition() {
        return DEFINITION;
    }

    @Override
    public void execute(LocalDateTime executeTime) {
        log.info("已完成定时任务调度链路心跳检测 | 业务执行时间={}", executeTime);
    }
}
