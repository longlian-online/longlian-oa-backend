package online.longlian.app.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${longlian.datasource.type:mysql}")
    private String datasourceType;

    @Value("${DB_HOST:}")
    private String dbHost;
    @Value("${DB_PORT:3306}")
    private String dbPort;
    @Value("${DB_DATABASE:}")
    private String dbName;
    @Value("${DB_USERNAME:root}")
    private String dbUser;
    @Value("${DB_PASSWORD:}")
    private String dbPass;

    @Bean
    public DataSource dataSource() {
        if ("sqlite".equalsIgnoreCase(datasourceType)) {
            org.sqlite.SQLiteDataSource ds = new org.sqlite.SQLiteDataSource();
            ds.setUrl("jdbc:sqlite:./data/longlian.db");
            return ds;
        } else {
            // MySQL + HikariCP
            HikariDataSource ds = new HikariDataSource();
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setJdbcUrl(String.format(
                    "jdbc:mysql://%s:%s/%s?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false",
                    dbHost, dbPort, dbName));
            ds.setUsername(dbUser);
            ds.setPassword(dbPass);
            return ds;
        }
    }
}
