package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.OrganizationAuthorizationService;
import online.longlian.app.service.common.OrganizationMembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

class OrganizationSwitchConcurrencyApiTest extends BaseApiTest {
    @SpyBean private UserMapper users;
    @SpyBean private OrganizationAuthorizationService authorization;
    @SpyBean private OrganizationMembershipService membership;
    @Autowired private SqlSessionTemplate sqlSession;
    private String ownerToken;
    private String memberToken;

    @BeforeEach
    void prepareOrganization() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_OWNER");
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        jdbcTemplate.update("UPDATE user SET default_org_id=1 WHERE id=2");
        ownerToken = loginAs("owner", "123456");
        memberToken = loginAs("member", "123456");
    }

    /** 生命周期事务清理默认组织后暂停提交，切换必须等待并拒绝已失效的关系。 */
    @ParameterizedTest
    @ValueSource(strings = {"dissolve", "remove", "exit"})
    void shouldRejectSwitchAfterLifecycleTransactionCommits(String mutation) throws Exception {
        CountDownLatch lifecycleUpdated = new CountDownLatch(1);
        CountDownLatch resumeLifecycle = new CountDownLatch(1);
        CountDownLatch switchReachedOrganization = new CountDownLatch(1);
        pauseFirstDefaultOrgUpdate(lifecycleUpdated, resumeLifecycle);
        doAnswer(call -> {
            if (lifecycleUpdated.getCount() == 0) switchReachedOrganization.countDown();
            return call.callRealMethod();
        }).when(authorization).lockOrganization(1L, true);
        // 旧实现没有组织锁：确保它读到未提交清理前的成员，从而稳定复现失效写回。
        doAnswer(call -> {
            Object member = call.callRealMethod();
            switchReachedOrganization.countDown();
            return member;
        }).when(membership).requireEnabledMember(2L, 1L);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> lifecycle = CompletableFuture.supplyAsync(() -> mutate(mutation), executor);
            CompletableFuture<Response> switching;
            try {
                assertThat(lifecycleUpdated.await(15, TimeUnit.SECONDS)).isTrue();
                switching = CompletableFuture.supplyAsync(this::switchOrganization, executor);
                assertThat(switchReachedOrganization.await(15, TimeUnit.SECONDS)).isTrue();
            } finally {
                resumeLifecycle.countDown();
            }
            lifecycle.get(15, TimeUnit.SECONDS).then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()));
            ResultCode expected = mutation.equals("dissolve") ? ResultCode.DATA_NOT_EXIT : ResultCode.OPERATION_FAIL;
            switching.get(15, TimeUnit.SECONDS).then().statusCode(200).body("code", equalTo(expected.getCode()));
        }
        assertLifecycleApplied(mutation);
    }

    /** 切换先更新默认组织但未提交时，生命周期随后完成并最终清空该默认组织。 */
    @ParameterizedTest
    @ValueSource(strings = {"dissolve", "remove", "exit"})
    void shouldClearDefaultOrgWhenLifecycleFollowsSwitch(String mutation) throws Exception {
        CountDownLatch switchUpdated = new CountDownLatch(1);
        CountDownLatch resumeSwitch = new CountDownLatch(1);
        CountDownLatch lifecycleReachedOrganization = new CountDownLatch(1);
        pauseFirstDefaultOrgUpdate(switchUpdated, resumeSwitch);
        doAnswer(call -> {
            if (switchUpdated.getCount() == 0) lifecycleReachedOrganization.countDown();
            return call.callRealMethod();
        }).when(authorization).lockOrganization(eq(1L), anyBoolean());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> switching = CompletableFuture.supplyAsync(this::switchOrganization, executor);
            CompletableFuture<Response> lifecycle;
            try {
                assertThat(switchUpdated.await(15, TimeUnit.SECONDS)).isTrue();
                lifecycle = CompletableFuture.supplyAsync(() -> mutate(mutation), executor);
                assertThat(lifecycleReachedOrganization.await(15, TimeUnit.SECONDS)).isTrue();
            } finally {
                resumeSwitch.countDown();
            }
            switching.get(15, TimeUnit.SECONDS).then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                    .body("data.id", equalTo("1")).body("data.role", equalTo("ORG_USER"));
            lifecycle.get(15, TimeUnit.SECONDS).then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        }
        assertLifecycleApplied(mutation);
    }

    private void pauseFirstDefaultOrgUpdate(CountDownLatch updated, CountDownLatch resume) {
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            int affected = sqlSession.getMapper(UserMapper.class).update((User) call.getArgument(0), call.getArgument(1));
            if (first.compareAndSet(true, false)) {
                updated.countDown();
                assertThat(resume.await(15, TimeUnit.SECONDS)).isTrue();
            }
            return affected;
        }).when(users).update(isNull(), argThat(query -> query != null && query.getSqlSet().contains("default_org_id")));
    }

    private Response switchOrganization() {
        return userRequest(memberToken).body(Map.of("orgId", 1L)).post("/app/user/switch");
    }

    private Response mutate(String mutation) {
        return switch (mutation) {
            case "dissolve" -> authRequest(ownerToken, 1L).delete("/orgadmin/organizations/1");
            case "remove" -> authRequest(ownerToken, 1L).delete("/orgadmin/members/2");
            case "exit" -> userRequest(memberToken).delete("/app/organizations/1/membership");
            default -> throw new IllegalArgumentException(mutation);
        };
    }

    private void assertLifecycleApplied(String mutation) {
        assertThat(jdbcTemplate.queryForObject("SELECT default_org_id FROM user WHERE id=2", Long.class)).isZero();
        String deleted = mutation.equals("dissolve") ? "SELECT deleted_at FROM organization WHERE id=1"
                : "SELECT deleted_at FROM organization_member WHERE id=2";
        assertThat(jdbcTemplate.queryForObject(deleted, Object.class)).isNotNull();
    }
}
