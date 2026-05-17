package online.longlian.app.api.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCleanupUtil {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ResourcePatternResolver resourcePatternResolver;

    public void initSchema() {
        verifyDatabaseConnection();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user'",
                Integer.class
        );
        if (count != null && count > 0) {
            log.info("测试数据库表已存在，跳过建表");
            return;
        }

        log.info("开始执行测试数据库建表脚本...");
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath*:manifest/migrate/*.sql");
            List<Resource> sortedResources = Arrays.stream(resources)
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .toList();

            for (Resource resource : sortedResources) {
                log.info("执行迁移文件: {}", resource.getFilename());
                try (Connection conn = dataSource.getConnection()) {
                    ScriptUtils.executeSqlScript(conn, resource);
                }
            }
            log.info("建表脚本执行完毕");
        } catch (IOException e) {
            throw new RuntimeException("扫描迁移文件失败", e);
        } catch (SQLException e) {
            throw new RuntimeException("测试数据库建表失败", e);
        }
    }

    private void verifyDatabaseConnection() {
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            try (Connection conn = dataSource.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    log.info("数据库连接验证成功");
                    return;
                }
            } catch (SQLException e) {
                log.warn("数据库连接验证失败（第{}次尝试）: {} - {}",
                        i + 1, e.getClass().getSimpleName(), e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("数据库连接验证被中断", ie);
                    }
                }
            }
        }
        throw new RuntimeException("数据库连接验证失败，已重试 " + maxRetries + " 次，请检查 MySQL 和 Redis 服务是否正常运行");
    }

    public void truncateAllTables() {
        List<String> tableNames = getAllTableNames();
        if (tableNames.isEmpty()) {
            log.warn("未检测到任何表，跳过清理");
            return;
        }

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : tableNames) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`");
            } catch (Exception e) {
                log.warn("TRUNCATE 表 {} 失败: {}", table, e.getMessage());
            }
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        log.debug("已清空 {} 张表", tableNames.size());
    }

    private List<String> getAllTableNames() {
        return jdbcTemplate.query(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'",
                (rs, rowNum) -> rs.getString("table_name")
        );
    }
}
