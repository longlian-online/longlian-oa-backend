package online.longlian.app.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Value("${longlian.datasource.type:mysql}")
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
            case "mysql" -> DbType.MYSQL;
            case "sqlite" -> DbType.SQLITE;
            default -> throw new IllegalArgumentException("不支持的数据库类型: " + type);
        };
    }
}
