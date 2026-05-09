package online.longlian.app.scheduled;

import online.longlian.app.pojo.bo.ScheduledTaskDefinition;

import java.time.LocalDateTime;

/**
 * 定时任务接口。
 * <p>
 * 所有定时任务必须实现此接口：
 * <ol>
 *   <li>{@link #getDefinition()} 提供任务元数据（名称、cron、描述等），引擎据此注册调度</li>
 *   <li>{@link #execute(LocalDateTime)} 执行具体业务逻辑，时间由上层注入，严禁内部调用 {@code LocalDateTime.now()}</li>
 * </ol>
 * <p>
 * 实现类用 {@code @Component} 注解即可自动注册到调度引擎。
 */
public interface ScheduledTask {

    ScheduledTaskDefinition getDefinition();

    void execute(LocalDateTime executeTime);
}
