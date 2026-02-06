package online.longlian.app.common.config;

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
    @ConditionalOnProperty(
            prefix = "longlian.datasource",
            name = "type",
            havingValue = "mysql",
            matchIfMissing = true
    )
    public DruidDataSource mysqlDataSource(
            @Value("${DB_HOST}") String host,
            @Value("${DB_PORT}") int port,
            @Value("${DB_DATABASE}") String database,
            @Value("${DB_USERNAME}") String username,
            @Value("${DB_PASSWORD}") String password
    ) {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false");
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
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
