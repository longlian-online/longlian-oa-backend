package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.entity.OrganizationMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

class JoinApplicationConcurrencyApiTest extends BaseApiTest {
    @SpyBean private OrganizationMapper organizations;
    @SpyBean private GroupApplicationMapper applications;
    @SpyBean private OrganizationMemberMapper members;
    @Autowired private SqlSessionTemplate sqlSession;
    private String managerToken;

    @BeforeEach
    void prepareOrganization() {
        createUserWithOrganization(1L, "manager", "123456", "manager@example.com", 1L, 1L, "ORG_OWNER");
        managerToken = loginAs("manager", "123456");
        createOrganizationUserInviteOTP("JOIN01", 1L);
        createOrganizationUserInviteOTP("JOIN02", 1L);
    }

    /** 旧快照请求在另一个注册申请提交后必须返回业务失败，不能让唯一键冲突成为系统错误。 */
    @Test
    void shouldRejectRegistrationCommittedAfterEarlierSnapshot() throws Exception {
        createEmailVerifyOTP("MAIL01", 1L, "candidate@example.com");
        createEmailVerifyOTP("MAIL02", 1L, "candidate@example.com");
        Response later = afterEarlierSnapshot(
                () -> register("JOIN02", "MAIL02", "lateruser"),
                () -> register("JOIN01", "MAIL01", "firstuser").then()
                        .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode())));

        later.then().statusCode(200).body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM group_application WHERE org_id=1 AND status=0", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE email='candidate@example.com'", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM one_time_password WHERE code='MAIL02'", Integer.class))
                .isZero();
    }

    /** 同一已有用户用两张邀请码提交时，组织锁后的当前读必须看见先提交的待审申请。 */
    @Test
    void shouldRejectExistingUserApplicationCommittedAfterEarlierSnapshot() throws Exception {
        String token = prepareExistingUser();
        Response later = afterEarlierSnapshot(
                () -> join(token, "JOIN02"),
                () -> join(token, "JOIN01").then().statusCode(200)
                        .body("code", equalTo(ResultCode.SUCCESS.getCode())));

        later.then().statusCode(200).body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM group_application WHERE org_id=1 AND user_id=2 AND status=0", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM one_time_password WHERE code='JOIN02'", Integer.class))
                .isZero();
    }

    /** 在等待期间先提交并批准另一申请时，旧快照请求必须识别新成员，不能再留下待审申请。 */
    @Test
    void shouldRejectMemberApprovedAfterEarlierSnapshot() throws Exception {
        String token = prepareExistingUser();
        Response later = afterEarlierSnapshot(() -> join(token, "JOIN02"), () -> {
            join(token, "JOIN01").then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
            Long applicationId = jdbcTemplate.queryForObject("SELECT id FROM group_application WHERE user_id=2", Long.class);
            authRequest(managerToken, 1L).body(Map.of("applicationStatus", "APPROVED"))
                    .put("/orgadmin/members/applications/" + applicationId + "/review")
                    .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        });

        later.then().statusCode(200).body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM group_application WHERE org_id=1 AND user_id=2 AND status=0", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member WHERE org_id=1 AND user_id=2 AND deleted_at IS NULL", Integer.class))
                .isEqualTo(1);
    }

    /** 组织治理审批与提交按组织锁串行，重复提交在审批提交后识别最新成员。 */
    @Test
    void shouldSerializeApprovalAndDuplicateSubmissionByOrganization() throws Exception {
        String token = prepareExistingUser();
        join(token, "JOIN01").then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long applicationId = jdbcTemplate.queryForObject("SELECT id FROM group_application WHERE user_id = 2", Long.class);
        CountDownLatch approvalBeforeMemberInsert = new CountDownLatch(1);
        CountDownLatch duplicateBeforeOrganizationLock = new CountDownLatch(1);
        doAnswer(call -> {
            approvalBeforeMemberInsert.countDown();
            assertThat(duplicateBeforeOrganizationLock.await(15, TimeUnit.SECONDS)).isTrue();
            return sqlSession.getMapper(OrganizationMemberMapper.class)
                    .insert(call.getArgument(0, OrganizationMember.class));
        }).when(members).insert(any(OrganizationMember.class));
        doAnswer(call -> {
            if (approvalBeforeMemberInsert.getCount() == 0) {
                duplicateBeforeOrganizationLock.countDown();
            }
            return call.callRealMethod();
        }).when(organizations).selectOne(argThat(query ->
                query != null && query.getSqlSegment().endsWith("FOR UPDATE")));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> approval = CompletableFuture.supplyAsync(() ->
                    authRequest(managerToken, 1L).body(Map.of("applicationStatus", "APPROVED"))
                            .put("/orgadmin/members/applications/" + applicationId + "/review"), executor);
            try {
                assertThat(approvalBeforeMemberInsert.await(15, TimeUnit.SECONDS)).isTrue();
                join(token, "JOIN02").then().statusCode(200)
                        .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
            } finally {
                duplicateBeforeOrganizationLock.countDown();
            }
            approval.get(15, TimeUnit.SECONDS).then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        }

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM group_application WHERE id = ?",
                Integer.class, applicationId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member WHERE org_id = 1 AND user_id = 2",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM one_time_password WHERE code = 'JOIN02'",
                Integer.class)).isZero();
    }

    private Response afterEarlierSnapshot(Supplier<Response> delayedRequest, Runnable interveningWrite) throws Exception {
        CountDownLatch snapshotEstablished = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            if (first.compareAndSet(true, false)) {
                snapshotEstablished.countDown();
                assertThat(resume.await(15, TimeUnit.SECONDS)).isTrue();
            }
            return call.callRealMethod();
        }).when(organizations).selectOne(argThat(query ->
                query != null && query.getSqlSegment().endsWith("FOR UPDATE")));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> delayed = CompletableFuture.supplyAsync(delayedRequest, executor);
            try {
                assertThat(snapshotEstablished.await(15, TimeUnit.SECONDS)).isTrue();
                interveningWrite.run();
            } finally {
                resume.countDown();
            }
            return delayed.get(15, TimeUnit.SECONDS);
        }
    }

    /** 没有成员唯一索引时，并发审批同用户的不同历史申请也只能创建一条有效成员。 */
    @Test
    void shouldCreateOnlyOneMembershipWhenApprovingConcurrentApplications() throws Exception {
        prepareExistingUser();
        jdbcTemplate.update("INSERT INTO group_application(id,org_id,user_id,status,application_type) VALUES(10,1,2,0,1),(11,1,2,0,1)");
        CountDownLatch firstBeforeInsert = new CountDownLatch(1);
        CountDownLatch secondBeforeOrganizationLock = new CountDownLatch(1);
        doAnswer(call -> {
            firstBeforeInsert.countDown();
            assertThat(secondBeforeOrganizationLock.await(15, TimeUnit.SECONDS)).isTrue();
            return sqlSession.getMapper(OrganizationMemberMapper.class)
                    .insert(call.getArgument(0, OrganizationMember.class));
        }).when(members).insert(any(OrganizationMember.class));
        doAnswer(call -> {
            if (firstBeforeInsert.getCount() == 0) secondBeforeOrganizationLock.countDown();
            return call.callRealMethod();
        }).when(organizations).selectOne(argThat(query ->
                query != null && query.getSqlSegment().endsWith("FOR UPDATE")));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> first = CompletableFuture.supplyAsync(() ->
                    authRequest(managerToken, 1L).body(Map.of("applicationStatus", "APPROVED"))
                            .put("/orgadmin/members/applications/10/review"), executor);
            try {
                assertThat(firstBeforeInsert.await(15, TimeUnit.SECONDS)).isTrue();
                authRequest(managerToken, 1L).body(Map.of("applicationStatus", "APPROVED"))
                        .put("/orgadmin/members/applications/11/review").then().statusCode(200)
                        .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
            } finally {
                secondBeforeOrganizationLock.countDown();
            }
            first.get(15, TimeUnit.SECONDS).then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        }
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member WHERE org_id=1 AND user_id=2 AND deleted_at IS NULL", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM group_application WHERE id=10", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM group_application WHERE id=11", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT reviewer_id FROM group_application WHERE id=11", Long.class)).isNull();
    }

    private String prepareExistingUser() {
        createTestUser(2L, "candidate", "123456", "candidate@example.com");
        return loginAs("candidate", "123456");
    }

    private Response register(String invite, String code, String username) {
        return request().body(Map.of("inviteCode", invite, "code", code, "username", username,
                "password", "123456", "nickname", "Candidate", "email", "candidate@example.com"))
                .post("/app/user/register/join-organization");
    }

    private Response join(String token, String invite) {
        return userRequest(token).body(Map.of("inviteCode", invite)).post("/app/user/organizations/join-by-invite");
    }
}
