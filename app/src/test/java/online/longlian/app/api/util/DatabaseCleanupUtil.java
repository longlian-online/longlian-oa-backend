package online.longlian.app.api.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCleanupUtil {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public void initSchema() {
        verifyDatabaseConnection();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'app_user'",
                Integer.class
        );
        if (count != null && count > 0) {
            log.info("测试数据库表已存在，跳过建表");
            return;
        }

        Path schemaFile = findSchemaFile();
        log.info("开始执行测试数据库建表脚本: {}", schemaFile);
        try {
            String sql = Files.readString(schemaFile);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
            log.info("建表脚本执行完毕");
        } catch (SQLException | IOException e) {
            throw new RuntimeException("测试数据库建表失败", e);
        }
    }

    private Path findSchemaFile() {
        Path currentDirectory = Path.of("").toAbsolutePath();
        while (currentDirectory != null) {
            Path schemaFile = currentDirectory.resolve("db/schema.sql");
            if (Files.isRegularFile(schemaFile)) {
                return schemaFile;
            }
            currentDirectory = currentDirectory.getParent();
        }
        throw new IllegalStateException("未找到 db/schema.sql，当前测试工作目录: " + Path.of("").toAbsolutePath());
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
        throw new RuntimeException("数据库连接验证失败，已重试 " + maxRetries + " 次，请检查 PostgreSQL 和 Redis 服务是否正常运行");
    }

    public void truncateAllTables() {
        List<String> tableNames = getAllTableNames();
        if (tableNames.isEmpty()) {
            log.warn("未检测到任何表，跳过清理");
            return;
        }

        for (String table : tableNames) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE \"" + table + "\" CASCADE");
            } catch (Exception e) {
                log.warn("TRUNCATE 表 {} 失败: {}", table, e.getMessage());
            }
        }
        log.debug("已清空 {} 张表", tableNames.size());
    }

    private List<String> getAllTableNames() {
        return jdbcTemplate.query(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'",
                (rs, rowNum) -> rs.getString("table_name")
        );
    }
}
