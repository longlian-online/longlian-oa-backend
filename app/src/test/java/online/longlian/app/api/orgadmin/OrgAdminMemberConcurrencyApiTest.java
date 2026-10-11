package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.common.OrganizationAuthorizationService;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.AopTestUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;

class OrgAdminMemberConcurrencyApiTest extends BaseApiTest {
    @SpyBean private OrganizationAuthorizationService authorization;
    @SpyBean private OrganizationMemberMapper members;
    @Autowired private SqlSessionTemplate sqlSession;

    /** 两个请求通过所有者入口鉴权，先提交者完成转让后，旧所有者的另一个请求被拒绝。 */
    @Test
    void shouldCommitOnlyOneOwnershipTransfer() throws Exception {
        prepare();
        String owner = loginAs("owner", "123456");
        CountDownLatch blocked = new CountDownLatch(1), resume = new CountDownLatch(1);
        pauseBeforeFirstOrganizationLock(blocked, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> stale = CompletableFuture.supplyAsync(
                    () -> authRequest(owner, 1L).put("/orgadmin/members/4/ownership"), executor);
            try {
                assertThat(blocked.await(15, TimeUnit.SECONDS)).isTrue();
                authRequest(owner, 1L).put("/orgadmin/members/2/ownership")
                        .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
            } finally { resume.countDown(); }
            stale.get(15, TimeUnit.SECONDS).then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        }
        assertThat(role(2L)).isEqualTo("ORG_OWNER");
        assertThat(role(4L)).isEqualTo("ORG_USER");
        assertThat(ownerCount()).isEqualTo(1);
    }

    /** 管理员状态请求暂停后，所有者先提升目标；恢复时必须按最新目标角色拒绝操作。 */
    @Test
    void shouldRejectDisableAfterTargetBecomesAdmin() throws Exception {
        prepare();
        String owner = loginAs("owner", "123456"), admin = loginAs("member2", "123456");
        CountDownLatch blocked = new CountDownLatch(1), resume = new CountDownLatch(1);
        pauseBeforeFirstOrganizationLock(blocked, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> stale = CompletableFuture.supplyAsync(() -> authRequest(admin, 1L)
                    .body(Map.of("status", "DISABLED")).patch("/orgadmin/members/3/status"), executor);
            try {
                assertThat(blocked.await(15, TimeUnit.SECONDS)).isTrue();
                authRequest(owner, 1L).body(Map.of("orgRole", "ORG_ADMIN")).patch("/orgadmin/members/3/role")
                        .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
            } finally { resume.countDown(); }
            stale.get(15, TimeUnit.SECONDS).then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        }
        assertThat(role(3L)).isEqualTo("ORG_ADMIN");
        assertThat(status(3L)).isEqualTo(1);
    }

    /** 请求已通过入口鉴权，但操作者随后被降级，事务内仍必须重新授权。 */
    @Test
    void shouldRejectDemotedOperatorAfterAuthentication() throws Exception {
        prepare();
        String owner = loginAs("owner", "123456"), admin = loginAs("member2", "123456");
        CountDownLatch blocked = new CountDownLatch(1), resume = new CountDownLatch(1);
        pauseBeforeFirstOrganizationLock(blocked, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> stale = CompletableFuture.supplyAsync(() -> authRequest(admin, 1L)
                    .body(Map.of("status", "DISABLED")).patch("/orgadmin/members/4/status"), executor);
            try {
                assertThat(blocked.await(15, TimeUnit.SECONDS)).isTrue();
                authRequest(owner, 1L).body(Map.of("orgRole", "ORG_USER")).patch("/orgadmin/members/2/role")
                        .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
            } finally { resume.countDown(); }
            stale.get(15, TimeUnit.SECONDS).then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        }
        assertThat(role(2L)).isEqualTo("ORG_USER");
        assertThat(status(4L)).isEqualTo(1);
    }

    /** 同组织两个合法治理请求排队提交，不能因为外层零等待 Redis 锁而失败。 */
    @Test
    void shouldCommitBothConcurrentIndependentMemberChanges() throws Exception {
        prepare();
        String admin = loginAs("member2", "123456");
        CountDownLatch updated = new CountDownLatch(1), resume = new CountDownLatch(1);
        CountDownLatch secondReachedLock = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            int result = sqlSession.getMapper(OrganizationMemberMapper.class)
                    .update((OrganizationMember) call.getArgument(0), call.getArgument(1));
            if (first.compareAndSet(true, false)) {
                updated.countDown();
                assertThat(resume.await(15, TimeUnit.SECONDS)).isTrue();
            }
            return result;
        }).when(members).update(isNull(), any());
        doAnswer(call -> {
            if (updated.getCount() == 0) secondReachedLock.countDown();
            return call.callRealMethod();
        }).when(authorizationTarget()).lockOrganization(1L, true);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> firstChange = CompletableFuture.supplyAsync(() -> authRequest(admin, 1L)
                    .body(Map.of("status", "DISABLED")).patch("/orgadmin/members/3/status"), executor);
            CompletableFuture<Response> secondChange;
            try {
                assertThat(updated.await(15, TimeUnit.SECONDS)).isTrue();
                secondChange = CompletableFuture.supplyAsync(() -> authRequest(admin, 1L)
                        .body(Map.of("status", "DISABLED")).patch("/orgadmin/members/4/status"), executor);
                assertThat(secondReachedLock.await(15, TimeUnit.SECONDS)).isTrue();
            } finally { resume.countDown(); }
            firstChange.get(15, TimeUnit.SECONDS).then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
            secondChange.get(15, TimeUnit.SECONDS).then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        }
        assertThat(status(3L)).isZero();
        assertThat(status(4L)).isZero();
    }

    /** 转让第二次数据库写入失败时，第一次降级也必须回滚，不能留下无所有者组织。 */
    @Test
    void shouldRollbackBothRolesWhenOwnershipPromotionFails() {
        prepare();
        String owner = loginAs("owner", "123456");
        AtomicInteger writes = new AtomicInteger();
        doAnswer(call -> {
            if (writes.incrementAndGet() == 2) throw new DataIntegrityViolationException("promotion failure");
            return sqlSession.getMapper(OrganizationMemberMapper.class)
                    .update((OrganizationMember) call.getArgument(0), call.getArgument(1));
        }).when(members).update(isNull(), any());
        authRequest(owner, 1L).put("/orgadmin/members/2/ownership")
                .then().statusCode(200).body("code", equalTo(ResultCode.FAIL.getCode()));
        assertThat(role(1L)).isEqualTo("ORG_OWNER");
        assertThat(role(2L)).isEqualTo("ORG_ADMIN");
        assertThat(ownerCount()).isEqualTo(1);
    }

    /** 两份历史待审申请同时通过时，当前读必须阻止第二份创建重复有效成员。 */
    @Test
    void shouldApproveOnlyOneConcurrentMembershipApplication() throws Exception {
        prepare();
        createTestUser(5L, "applicant", "123456", "applicant@example.com");
        jdbcTemplate.update("INSERT INTO group_application(id,org_id,user_id,status,application_type) VALUES(10,1,5,0,1),(11,1,5,0,1)");
        String owner = loginAs("owner", "123456");
        CountDownLatch blocked = new CountDownLatch(1), resume = new CountDownLatch(1);
        pauseBeforeFirstOrganizationLock(blocked, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> stale = CompletableFuture.supplyAsync(() -> authRequest(owner, 1L)
                    .body(Map.of("applicationStatus", "APPROVED")).put("/orgadmin/members/applications/11/review"), executor);
            try {
                assertThat(blocked.await(15, TimeUnit.SECONDS)).isTrue();
                authRequest(owner, 1L).body(Map.of("applicationStatus", "APPROVED"))
                        .put("/orgadmin/members/applications/10/review")
                        .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
            } finally { resume.countDown(); }
            stale.get(15, TimeUnit.SECONDS).then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE user_id=5 AND org_id=1 AND deleted_at IS NULL", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM group_application WHERE id=11", Integer.class)).isZero();
    }

    /** 不同组织审批同一用户时，成员索引间隙不能与用户行锁形成跨组织死锁。 */
    @Test
    void shouldApproveMembershipsAcrossOrganizationsWithoutGapLockDeadlock() throws Exception {
        prepare();
        createOrganization(2L, "second");
        createOrganizationMember(10L, 2L, 1L, "ORG_OWNER");
        createTestUser(5L, "applicant", "123456", "applicant@example.com");
        jdbcTemplate.update("INSERT INTO group_application(id,org_id,user_id,status,application_type) VALUES(10,1,5,0,1),(11,2,5,0,1)");
        String owner = loginAs("owner", "123456");
        CountDownLatch firstCheckedMembership = new CountDownLatch(1), resume = new CountDownLatch(1);
        CountDownLatch secondAuthorized = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            OrganizationMember result = sqlSession.getMapper(OrganizationMemberMapper.class).selectOne(call.getArgument(0));
            if (result == null && first.compareAndSet(true, false)) {
                firstCheckedMembership.countDown();
                assertThat(resume.await(15, TimeUnit.SECONDS)).isTrue();
            }
            return result;
        }).when(members).selectOne(any());
        doAnswer(call -> {
            Object result = call.callRealMethod();
            secondAuthorized.countDown();
            return result;
        }).when(authorizationTarget()).requireManager(2L, 1L);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> firstApproval = CompletableFuture.supplyAsync(() -> authRequest(owner, 1L)
                    .body(Map.of("applicationStatus", "APPROVED")).put("/orgadmin/members/applications/10/review"), executor);
            CompletableFuture<Response> secondApproval;
            try {
                assertThat(firstCheckedMembership.await(15, TimeUnit.SECONDS)).isTrue();
                secondApproval = CompletableFuture.supplyAsync(() -> authRequest(owner, 2L)
                        .body(Map.of("applicationStatus", "APPROVED")).put("/orgadmin/members/applications/11/review"), executor);
                assertThat(secondAuthorized.await(15, TimeUnit.SECONDS)).isTrue();
            } finally { resume.countDown(); }
            firstApproval.get(15, TimeUnit.SECONDS).then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
            secondApproval.get(15, TimeUnit.SECONDS).then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        }
        assertThat(jdbcTemplate.queryForList("SELECT org_id FROM organization_member WHERE user_id=5 AND deleted_at IS NULL ORDER BY org_id", Long.class))
                .containsExactly(1L, 2L);
    }

    private void prepare() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_OWNER");
        for (long id = 2; id <= 4; id++) {
            createTestUser(id, "member" + id, "123456", "member" + id + "@example.com");
            createOrganizationMember(id, 1L, id, id == 2 ? "ORG_ADMIN" : "ORG_USER");
        }
    }

    private OrganizationAuthorizationService authorizationTarget() {
        // 配置竞态钩子时绕过事务代理；实际 HTTP 请求仍通过 MANDATORY 代理。
        return AopTestUtils.getUltimateTargetObject(authorization);
    }

    private void pauseBeforeFirstOrganizationLock(CountDownLatch blocked, CountDownLatch resume) {
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            if (first.compareAndSet(true, false)) {
                blocked.countDown();
                assertThat(resume.await(15, TimeUnit.SECONDS)).isTrue();
            }
            return call.callRealMethod();
        }).when(authorizationTarget()).lockOrganization(1L, true);
    }

    private String role(Long id) {
        return jdbcTemplate.queryForObject("SELECT org_role FROM organization_member WHERE id=?", String.class, id);
    }

    private int status(Long id) {
        return jdbcTemplate.queryForObject("SELECT status FROM organization_member WHERE id=?", Integer.class, id);
    }

    private int ownerCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE org_id=1 AND org_role='ORG_OWNER' AND deleted_at IS NULL", Integer.class);
    }
}
