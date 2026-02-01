package online.longlian.app.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    @Bean
    @ConditionalOnProperty(
            prefix = "longlian.datasource",
            name = "type",
            havingValue = "sqlite"
    )
    public DataSource sqliteDataSource(
            @Value("${sqlite.datasource.url}") String sqliteUrl
    ) {
        org.sqlite.SQLiteDataSource ds = new org.sqlite.SQLiteDataSource();
        ds.setUrl(sqliteUrl);
        return ds;
    }

    @Bean(destroyMethod = "close")
    @ConfigurationProperties(prefix = "spring.datasource")
    @ConditionalOnProperty(
            prefix = "longlian.datasource",
            name = "type",
            havingValue = "mysql",
            matchIfMissing = true
    )
    public DruidDataSource mysqlDataSource() {
        return new DruidDataSource();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "longlian.datasource",
            name = "type",
            havingValue = "mysql",
            matchIfMissing = true
    )
    public DataSource dataSource(DruidDataSource mysqlDataSource) {
        return mysqlDataSource;
    }
}
