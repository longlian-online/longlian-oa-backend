package online.longlian.app.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    @Primary
    @ConditionalOnProperty(
            prefix = "longlian.datasource",
            name = "type",
            havingValue = "mysql",
            matchIfMissing = true
    )
    public DataSource mysqlDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password:}") String password
    ) {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }
}
