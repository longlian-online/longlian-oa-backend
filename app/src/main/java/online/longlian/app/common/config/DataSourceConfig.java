package online.longlian.app.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean(destroyMethod = "close")
    public DruidDataSource dataSource(
            @Value("${DB_HOST}") String host,
            @Value("${DB_PORT}") int port,
            @Value("${DB_DATABASE}") String database,
            @Value("${DB_USERNAME}") String username,
            @Value("${DB_PASSWORD}") String password
    ) {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + database + "?stringtype=unspecified");
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }
}
