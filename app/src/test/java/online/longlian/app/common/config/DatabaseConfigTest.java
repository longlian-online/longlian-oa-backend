package online.longlian.app.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mariadb.jdbc.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseConfigTest {

    /** 已提交的运行模板必须显式选择 MariaDB，避免环境间仍使用旧 MySQL 协议。 */
    @Test
    void shouldUseMariaDbInApplicationConfigurations() throws IOException, SQLException {
        for (String resource : new String[]{"application.yml.example", "application-test.yml"}) {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                    .load(resource, new ClassPathResource(resource));

            assertThat(sources).isNotEmpty();
            PropertySource<?> source = sources.getFirst();
            assertThat(source.getProperty("longlian.datasource.type")).isEqualTo("mariadb");
            String jdbcUrl = source.getProperty("spring.datasource.url").toString();
            assertThat(jdbcUrl)
                    .startsWith("jdbc:mariadb://")
                    .contains("timezone=+08:00")
                    .doesNotContain("timezone=%2B08:00");
            String timezone = Configuration.parse(jdbcUrl).timezone();
            assertThat(timezone).isEqualTo("+08:00");
            assertThatCode(() -> ZoneId.of(timezone)).doesNotThrowAnyException();
        }
    }

    /** MariaDB 数据源必须使用专用驱动，避免驱动和 JDBC 协议发生隐式兼容。 */
    @Test
    void shouldConfigureMariaDbDataSource() {
        DataSource dataSource = new DataSourceConfig().mariadbDataSource(
                "jdbc:mariadb://localhost:3306/longlian_oa",
                "longlian",
                "secret"
        );

        assertThat(dataSource).isInstanceOf(DruidDataSource.class);
        DruidDataSource druidDataSource = (DruidDataSource) dataSource;
        assertThat(druidDataSource.getDriverClassName()).isEqualTo("org.mariadb.jdbc.Driver");
        assertThat(druidDataSource.getUrl()).startsWith("jdbc:mariadb://");
        assertThat(druidDataSource.getUsername()).isEqualTo("longlian");
        assertThat(druidDataSource.getPassword()).isEqualTo("secret");
        druidDataSource.close();
    }

    /** 分页插件必须显式使用 MariaDB 方言，避免依赖 MySQL 兼容分支。 */
    @Test
    void shouldUseMariaDbPaginationDialect() {
        MybatisPlusConfig config = new MybatisPlusConfig();
        ReflectionTestUtils.setField(config, "datasourceType", "mariadb");

        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();

        assertThat(interceptor.getInterceptors()).singleElement()
                .isInstanceOfSatisfying(PaginationInnerInterceptor.class,
                        pagination -> assertThat(pagination.getDbType()).isEqualTo(DbType.MARIADB));
    }

    /** 旧 MySQL 类型必须显式失败，避免部署时静默回退到错误方言。 */
    @Test
    void shouldRejectLegacyMySqlDialect() {
        MybatisPlusConfig config = new MybatisPlusConfig();
        ReflectionTestUtils.setField(config, "datasourceType", "mysql");

        assertThatThrownBy(config::mybatisPlusInterceptor)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不支持的数据库类型: mysql");
    }
}
