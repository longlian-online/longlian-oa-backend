/**
 * 定时任务配置
 */

package online.longlian.app.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;


@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * 定时任务调度器，使用虚拟线程执行
     */
    @Bean
    public SimpleAsyncTaskScheduler taskScheduler() {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setVirtualThreads(true);
        scheduler.setThreadNamePrefix("scheduled-task-");
        return scheduler;
    }
}
