package online.longlian.app.api.session;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.mapper.TokenBlacklistMapper;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.common.enumeration.TokenType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenRevocationApiTest extends BaseApiTest {
    @Autowired private TokenBlacklistService blacklist;
    @Autowired private JwtUtil jwt;
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

    /** 新签发的用户凭证可用，过期或格式非法的令牌一律拒绝。 */
    @Test
    void shouldAcceptFreshUserTokenAndRejectUnusableTokens() {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        assertThat(jwt.generateToken(1L)).isNotBlank();
        String token = jwt.generateToken(1L, "user");
        assertThat(jwt.validateToken(token)).isTrue();
        authRequest(token).get("/app/user/").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));

        String expired = Jwts.builder()
                .setSubject("1")
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(
                        "test-jwt-secret-key-must-be-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
        assertThat(jwt.validateToken(expired)).isFalse();
        assertThat(jwt.parseTokenIfValid(expired)).isNull();
        assertThat(jwt.parseTokenIfValid("not-a-token")).isNull();
        assertThat(jwt.getRemainingTimeSeconds("not-a-token")).isZero();
    }
}
