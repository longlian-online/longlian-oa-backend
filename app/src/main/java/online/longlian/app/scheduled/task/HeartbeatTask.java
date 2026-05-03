package online.longlian.app.scheduled.task;

import lombok.extern.slf4j.Slf4j;
import online.longlian.app.pojo.bo.ScheduledTaskDefinition;
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

    @Override
    public ScheduledTaskDefinition getDefinition() {
        return ScheduledTaskDefinition.builder()
                .taskName("heartbeat")
                .description("心跳检测任务，每 5 分钟执行一次，用于验证调度链路")
                .cronExpression("* 0/5 * * * ?")
                .enabled(false)
                .build();
    }

    @Override
    public void execute(LocalDateTime executeTime) {
        log.info("心跳检测 | executeTime={}", executeTime);
    }
}
