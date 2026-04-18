package online.longlian.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication()
@MapperScan("online.longlian.app.mapper")
@EnableCaching
@EnableMethodSecurity(prePostEnabled = true)
@ConfigurationPropertiesScan
public class AppApplication {

    public static void main(String[] args) {
        // 启动子线程时自动拷贝父线程上下文
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        SpringApplication.run(AppApplication.class, args);
    }
}
