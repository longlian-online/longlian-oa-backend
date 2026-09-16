package online.longlian.app.api.session;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.mapper.TokenBlacklistMapper;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.app.service.impl.TokenRevocationStore;
import online.longlian.common.enumeration.TokenType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenRevocationApiTest extends BaseApiTest {
    @Autowired private TokenBlacklistService blacklist;
    @Autowired private JwtUtil jwt;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @SpyBean private TokenBlacklistMapper mapper;

    /** 退出登录会使已有缓存失效，并且只持久化 token 摘要。 */
    @Test
    void shouldRevokeWarmTokenAndPermitNewLogin() {
        createAdmin(1L, "admin", "123456", "root");
        String token = adminLoginAs("admin", "123456");
        authRequest(token).get("/admin/admins/").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).delete("/admin/session").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).get("/admin/admins/").then().body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT token FROM token_blacklist", String.class))
                .startsWith("sha256:").doesNotContain(token);
        String fresh = adminLoginAs("admin", "123456");
        assertThat(fresh).isNotEqualTo(token);
        authRequest(fresh).get("/admin/admins/").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /** 命中身份缓存后，后续请求无需查询黑名单表。 */
    @Test
    void shouldAvoidBlacklistSqlOnWarmRequests() {
        createAdmin(1L, "admin", "123456", "root");
        String token = adminLoginAs("admin", "123456");
        authRequest(token).get("/admin/admins/").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        clearInvocations(mapper);
        authRequest(token).get("/admin/admins/").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        verify(mapper, never()).selectList(any());
        verify(mapper, never()).selectCount(any());
    }

    /** 用户全量吊销不会影响相同数字 ID 的管理员。 */
    @Test
    void shouldIsolateUserAndAdminGlobalRevocations() {
        createAdmin(1L, "admin", "123456", "root");
        String adminToken = adminLoginAs("admin", "123456");
        String userToken = jwt.generateToken(1L, "user");
        blacklist.blacklistAllUserTokens(TokenType.User, 1L, "test kick");
        assertThat(blacklist.isBlacklisted(userToken)).isTrue();
        authRequest(adminToken).get("/admin/admins/").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String fresh = jwt.generateToken(1L, "user");
        assertThat(blacklist.isBlacklisted(fresh)).isFalse();
    }

    /** 并发冷读和退出登录不能恢复过期的授权缓存。 */
    @Test
    void shouldRetainRevocationAcrossConcurrentReads() {
        createAdmin(1L, "admin", "123456", "root");
        String token = adminLoginAs("admin", "123456");
        CompletableFuture<?> read = CompletableFuture.runAsync(() -> blacklist.isBlacklisted(token));
        blacklist.addToBlacklist(token, TokenType.Admin, 1L, "logout", 60);
        read.join();
        assertThat(blacklist.isBlacklisted(token)).isTrue();
        authRequest(token).get("/admin/admins/").then().body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /** 业务操作回滚时吊销记录也回滚，并释放缓存锁。 */
    @Test
    void shouldRollbackRevocationWithBusinessTransaction() {
        createAdmin(1L, "admin", "123456", "root");
        String token = adminLoginAs("admin", "123456");
        assertThat(blacklist.isBlacklisted(token)).isFalse();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            blacklist.addToBlacklist(token, TokenType.Admin, 1L, "rollback test", 60);
            throw new IllegalStateException("business rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM token_blacklist", Integer.class)).isZero();
        authRequest(token).get("/admin/admins/").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /** 历史数据迁移可以重复执行，并保留吊销记录。 */
    @Test
    void shouldMigrateLegacyTokensWithoutRestoringAccess() throws Exception {
        createAdmin(1L, "admin", "123456", "root");
        String token = adminLoginAs("admin", "123456");
        LocalDateTime longExpiry = testNow().plusHours(1);
        LocalDateTime shortExpiry = testNow().plusMinutes(1);
        jdbcTemplate.update("INSERT INTO token_blacklist (id,token,token_type,user_id,expired_at) VALUES (112,?,2,1,?)",
                token, longExpiry);
        jdbcTemplate.update("INSERT INTO token_blacklist (id,token,token_type,user_id,expired_at) VALUES (113,?,2,1,?)",
                TokenRevocationStore.digest(token), shortExpiry);
        Path script = Path.of("db/data-migrations/112-hash-token-blacklist.sql");
        if (!Files.isRegularFile(script)) {
            script = Path.of("../db/data-migrations/112-hash-token-blacklist.sql");
        }
        try (var connection = dataSource.getConnection()) {
            var resource = new EncodedResource(new FileSystemResource(script), StandardCharsets.UTF_8);
            ScriptUtils.executeSqlScript(connection, resource);
            ScriptUtils.executeSqlScript(connection, resource);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM token_blacklist WHERE token LIKE '%.%.%'", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM token_blacklist", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT expired_at FROM token_blacklist", LocalDateTime.class))
                .isAfter(testNow().plusMinutes(59));
        authRequest(token).get("/admin/admins/").then().body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }
}
