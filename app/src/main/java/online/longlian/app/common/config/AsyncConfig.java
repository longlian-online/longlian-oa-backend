package online.longlian.app.common.config;

import online.longlian.app.common.constants.CommonConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("verifyCodeExecutor")
    public Executor verifyCodeExecutor() {
        return new VirtualThreadTaskExecutor(CommonConstants.THREAD_NAME_PREFIX);
    }
}
