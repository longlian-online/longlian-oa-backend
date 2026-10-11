package online.longlian.app.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Value("${longlian.datasource.type:mariadb}")
    private String datasourceType;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        DbType dbType = resolveDbType(datasourceType);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        return interceptor;
    }

    private DbType resolveDbType(String type) {
        return switch (type.toLowerCase()) {
            case "mariadb" -> DbType.MARIADB;
            case "sqlite" -> DbType.SQLITE;
            default -> throw new IllegalArgumentException("不支持的数据库类型: " + type);
        };
    }
}
