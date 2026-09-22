package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.service.common.LockService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

public class OrgAdminMemberConcurrencyApiTest extends BaseApiTest {

    @SpyBean
    private LockService lockService;

    /**
     * 请求通过入口鉴权后暂停在组织锁前；另一个管理员先提交降级，恢复时必须按当前角色重新授权。
     */
    @Test
    void shouldRejectDemotedOperatorAfterAuthentication() throws Exception {
        createUserWithOrganization(1L, "firstadmin", "123456", "first@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "secondadmin", "123456", "second@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_ADMIN");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 1L, 2L);
        String firstToken = loginAs("firstadmin", "123456");
        String secondToken = loginAs("secondadmin", "123456");

        CountDownLatch blockedBeforeLock = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicBoolean blockFirst = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (blockFirst.compareAndSet(true, false)) {
                blockedBeforeLock.countDown();
                assertThat(resume.await(15, TimeUnit.SECONDS)).isTrue();
            }
            return invocation.callRealMethod();
        }).when(lockService).tryAcquireOrThrow(eq("org:member:role:1"), eq(0L), eq(TimeUnit.SECONDS));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> staleRequest = CompletableFuture.supplyAsync(() ->
                    authRequest(firstToken).body(Map.of("orgRole", "ORG_USER"))
                            .patch("/orgadmin/members/2/role"), executor);
            try {
                assertThat(blockedBeforeLock.await(15, TimeUnit.SECONDS)).isTrue();
                Response demotion = authRequest(secondToken).body(Map.of("orgRole", "ORG_USER"))
                        .patch("/orgadmin/members/1/role");
                demotion.then().statusCode(200);
                assertThat(demotion.jsonPath().getInt("code")).isEqualTo(ResultCode.SUCCESS.getCode());
            } finally {
                resume.countDown();
            }

            Response rejected = staleRequest.get(15, TimeUnit.SECONDS);
            rejected.then().statusCode(200);
            assertThat(rejected.jsonPath().getInt("code")).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT org_role FROM organization_member WHERE id = 1", String.class)).isEqualTo("ORG_USER");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT org_role FROM organization_member WHERE id = 2", String.class)).isEqualTo("ORG_ADMIN");
    }

    /**
     * 状态变更也要在锁内读取最新角色，否则可能禁用刚被提升的管理员。
     */
    @Test
    void shouldRejectDisableAfterTargetBecomesAdmin() throws Exception {
        createUserWithOrganization(1L, "firstadmin", "123456", "first@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String token = loginAs("firstadmin", "123456");

        CountDownLatch blockedBeforeLock = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicBoolean blockFirst = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (blockFirst.compareAndSet(true, false)) {
                blockedBeforeLock.countDown();
                assertThat(resume.await(15, TimeUnit.SECONDS)).isTrue();
            }
            return invocation.callRealMethod();
        }).when(lockService).tryAcquireOrThrow(eq("org:member:role:1"), eq(0L), eq(TimeUnit.SECONDS));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> staleRequest = CompletableFuture.supplyAsync(() ->
                    authRequest(token).body(Map.of("status", "DISABLED"))
                            .patch("/orgadmin/members/2/status"), executor);
            try {
                assertThat(blockedBeforeLock.await(15, TimeUnit.SECONDS)).isTrue();
                Response promotion = authRequest(token).body(Map.of("orgRole", "ORG_ADMIN"))
                        .patch("/orgadmin/members/2/role");
                promotion.then().statusCode(200);
                assertThat(promotion.jsonPath().getInt("code")).isEqualTo(ResultCode.SUCCESS.getCode());
            } finally {
                resume.countDown();
            }

            Response rejected = staleRequest.get(15, TimeUnit.SECONDS);
            rejected.then().statusCode(200);
            assertThat(rejected.jsonPath().getInt("code")).isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT org_role FROM organization_member WHERE id = 2", String.class)).isEqualTo("ORG_ADMIN");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM organization_member WHERE id = 2", Integer.class)).isEqualTo(1);
    }
}
