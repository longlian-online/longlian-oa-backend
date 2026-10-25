package online.longlian.app.api.app;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.pojo.entity.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;

class TaskClaimApiTest extends BaseApiTest {

    @SpyBean
    private ProjectWorkshopMapper workshops;
    @SpyBean
    private TaskInstanceMapper instances;
    @Autowired
    private SqlSessionTemplate sqlSession;

    private String token;

    @BeforeEach
    void createClaimFixture() {
        createUserWithOrganization(1L, "claimworker", "123456", "claimworker@example.com", 1L, 1L, "ORG_USER");
        createTestUser(2L, "claimowner", "123456", "claimowner@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '测试类型', 1, 2)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, metadata, resource_status, creator_id) "
                + "VALUES (1, 1, 1, '自动加入企划', '{}', 1, 2)");
        jdbcTemplate.update("INSERT INTO item (id, project_id, title, task_template_id, creator_id) "
                + "VALUES (1, 1, '接取项目', 0, 2)");
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) "
                + "VALUES (1, 1, '接取任务', '[]', 1, 2)");
        jdbcTemplate.update("INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) "
                + "VALUES (1, 1, 1, 0, '任务流')");
        jdbcTemplate.update("INSERT INTO item_task_node "
                + "(id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                + "VALUES (1, 1, 1, 1, 1, '任务节点', '[]', 1, 1)");
        createTaskInstance(1L);
        token = loginAs("claimworker", "123456");
    }

    /** 接取后企划进入个人工坊，任务进入可提交待办，不能只写入不可见的关联。 */
    @Test
    void shouldAutoJoinProjectWhenClaimingUnjoinedTask() {
        authRequest(token, 1L).body("{}").post("/app/workshop/list").then()
                .statusCode(200).body("code", equalTo(0)).body("data.total", equalTo(0));

        assertClaimSuccess(claim(token, 1L), true);

        assertClaimed(1L, 1L);
        assertThat(activeMemberships(1L)).isEqualTo(1);
        assertThat(membershipHistory(1L)).isEqualTo(1);

        authRequest(token, 1L).queryParam("bucket", "SUBMITTABLE").get("/app/task/instance/mine").then()
                .statusCode(200).body("code", equalTo(0))
                .body("data.list.instanceId", equalTo(List.of("1")))
                .body("data.list.projectId", equalTo(List.of("1")));
        authRequest(token, 1L).body("{}").post("/app/workshop/list").then()
                .statusCode(200).body("code", equalTo(0))
                .body("data.list.id", equalTo(List.of("1")));
    }

    /** 已加入时返回 false，原关联不能被替换或重复新增，接取事务仍须正常提交。 */
    @Test
    void shouldKeepExistingMembershipWhenClaimingTask() {
        createMembership(100L, 1L, false);

        assertClaimSuccess(claim(token, 1L), false);

        assertClaimed(1L, 1L);
        assertThat(activeMemberships(1L)).isEqualTo(1);
        assertThat(membershipHistory(1L)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT id FROM project_workshop WHERE project_id=1 AND user_id=1 AND deleted_at IS NULL", Long.class))
                .isEqualTo(100L);
    }

    /** 多条软删除历史不能阻止重新加入；历史保留，只新增一条有效关联。 */
    @Test
    void shouldRejoinProjectAfterMembershipWasSoftDeleted() {
        createMembership(100L, 1L, true);
        createMembership(101L, 1L, true);

        assertClaimSuccess(claim(token, 1L), true);

        assertClaimed(1L, 1L);
        assertThat(activeMemberships(1L)).isEqualTo(1);
        assertThat(membershipHistory(1L)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_workshop WHERE id IN (100,101) AND deleted_at IS NOT NULL", Integer.class))
                .isEqualTo(2);
    }

    /** 重复请求继续报业务失败，即使中途退出了企划，也不能因重复接取再加入。 */
    @Test
    void shouldRejectRepeatedClaimWithoutRejoiningProject() {
        assertClaimSuccess(claim(token, 1L), true);
        jdbcTemplate.update("UPDATE project_workshop SET deleted_at=NOW() WHERE project_id=1 AND user_id=1");

        assertClaimFailure(claim(token, 1L), ResultCode.OPERATION_FAIL);

        assertClaimed(1L, 1L);
        assertThat(activeMemberships(1L)).isZero();
        assertThat(membershipHistory(1L)).isEqualTo(1);
    }

    /** 两名成员从同一个 PENDING 快照竞争时仅 CAS 胜者成功且加入，败者无关联。 */
    @Test
    void shouldJoinOnlyWinnerWhenUsersCompeteForSameTask() throws Exception {
        String otherToken = loginAs("claimowner", "123456");
        synchronizeTaskSnapshots(2);

        List<Response> responses = concurrently(List.of(() -> claim(token, 1L), () -> claim(otherToken, 1L)));

        assertThat(responses.stream().map(response -> response.jsonPath().getInt("code")).toList())
                .containsExactlyInAnyOrder(ResultCode.SUCCESS.getCode(), ResultCode.OPERATION_FAIL.getCode());
        for (int index = 0; index < responses.size(); index++) {
            Response response = responses.get(index);
            long userId = index + 1L;
            if (response.jsonPath().getInt("code") == ResultCode.SUCCESS.getCode()) {
                assertClaimSuccess(response, true);
                assertClaimed(1L, userId);
                assertThat(activeMemberships(userId)).isEqualTo(1);
            } else {
                assertClaimFailure(response, ResultCode.OPERATION_FAIL);
                assertThat(activeMemberships(userId)).isZero();
            }
        }
    }

    /** 同一用户并发重复请求仍只有一个成功，不把失败改成幂等接取成功。 */
    @Test
    void shouldRejectConcurrentRepeatedClaim() throws Exception {
        synchronizeTaskSnapshots(2);

        List<Response> responses = concurrently(List.of(() -> claim(token, 1L), () -> claim(token, 1L)));

        assertThat(responses.stream().map(response -> response.jsonPath().getInt("code")).toList())
                .containsExactlyInAnyOrder(ResultCode.SUCCESS.getCode(), ResultCode.OPERATION_FAIL.getCode());
        for (Response response : responses) {
            if (response.jsonPath().getInt("code") == ResultCode.SUCCESS.getCode()) {
                assertClaimSuccess(response, true);
            } else {
                assertClaimFailure(response, ResultCode.OPERATION_FAIL);
            }
        }
        assertClaimed(1L, 1L);
        assertThat(activeMemberships(1L)).isEqualTo(1);
        assertThat(membershipHistory(1L)).isEqualTo(1);
    }

    /** 同人同企划不同槽位并发接取均成功，自动加入只发生一次。 */
    @Test
    void shouldJoinExactlyOnceWhenUserClaimsDifferentTasksConcurrently() throws Exception {
        createTaskInstance(2L);
        createTaskInstance(3L);
        synchronizeTaskSnapshots(3);

        List<Response> responses = concurrently(List.of(
                () -> claim(token, 1L), () -> claim(token, 2L), () -> claim(token, 3L)));

        for (Response response : responses) {
            response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        }
        assertThat(responses.stream().map(response -> response.jsonPath().getBoolean("data.autoJoinedProject")).toList())
                .containsExactlyInAnyOrder(true, false, false);
        assertThat(activeMemberships(1L)).isEqualTo(1);
        assertThat(membershipHistory(1L)).isEqualTo(1);
        assertClaimed(1L, 1L);
        assertClaimed(2L, 1L);
        assertClaimed(3L, 1L);
    }

    /** claim 已建立 RR 快照后手工加入提交，碰撞后的当前读仍须识别新关系并返回 false。 */
    @Test
    void shouldRecognizeManualJoinCommittedAfterClaimSnapshot() throws Exception {
        CountDownLatch claimBeforeInsert = new CountDownLatch(1);
        CountDownLatch resumeClaim = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            if (first.compareAndSet(true, false)) {
                claimBeforeInsert.countDown();
                assertThat(resumeClaim.await(15, TimeUnit.SECONDS)).isTrue();
            }
            return sqlSession.getMapper(ProjectWorkshopMapper.class)
                    .insert(call.getArgument(0, ProjectWorkshop.class));
        }).when(workshops).insert(any(ProjectWorkshop.class));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Response> delayedClaim = executor.submit(() -> claim(token, 1L));
            try {
                assertThat(claimBeforeInsert.await(15, TimeUnit.SECONDS)).isTrue();
                authRequest(token, 1L).post("/app/projects/1/workshop").then()
                        .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
            } finally {
                resumeClaim.countDown();
            }
            assertClaimSuccess(delayedClaim.get(30, TimeUnit.SECONDS), false);
        }
        assertClaimed(1L, 1L);
        assertThat(activeMemberships(1L)).isEqualTo(1);
        assertThat(membershipHistory(1L)).isEqualTo(1);
    }

    /** 已加入的快照读取后手工退出先提交，快速路径必须做当前读并实际重加，不能返回假 false。 */
    @Test
    void shouldRejoinWhenManualExitCommitsAfterMembershipSnapshot() throws Exception {
        createMembership(100L, 1L, false);
        CountDownLatch membershipSnapshot = new CountDownLatch(1);
        CountDownLatch resumeClaim = new CountDownLatch(1);
        doAnswer(call -> {
            Wrapper<ProjectWorkshop> query = call.getArgument(0);
            ProjectWorkshop snapshot = sqlSession.getMapper(ProjectWorkshopMapper.class).selectOne(query);
            membershipSnapshot.countDown();
            assertThat(resumeClaim.await(15, TimeUnit.SECONDS)).isTrue();
            return snapshot;
        }).when(workshops).selectOne(argThat(query ->
                query != null && !query.getSqlSegment().endsWith("LOCK IN SHARE MODE")));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Response> delayedClaim = executor.submit(() -> claim(token, 1L));
            try {
                assertThat(membershipSnapshot.await(15, TimeUnit.SECONDS)).isTrue();
                authRequest(token, 1L).delete("/app/projects/1/workshop").then()
                        .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
            } finally {
                resumeClaim.countDown();
            }
            assertClaimSuccess(delayedClaim.get(30, TimeUnit.SECONDS), true);
        }
        assertClaimed(1L, 1L);
        assertThat(activeMemberships(1L)).isEqualTo(1);
        assertThat(membershipHistory(1L)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM project_workshop WHERE id=100", java.sql.Timestamp.class))
                .isNotNull();
    }

    /** 手工加入也必须复用同一幂等实现，软删除历史不影响重加或重复调用。 */
    @Test
    void shouldKeepManualJoinIdempotentAfterSoftDelete() {
        createMembership(100L, 1L, true);
        for (int attempt = 0; attempt < 2; attempt++) {
            authRequest(token, 1L).post("/app/projects/1/workshop").then()
                    .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        }
        assertThat(activeMemberships(1L)).isEqualTo(1);
        assertThat(membershipHistory(1L)).isEqualTo(2);
        assertPending(1L);
    }

    /** 无认证不能接取或留下加入记录。 */
    @Test
    void shouldRejectClaimWithoutAuthentication() {
        assertClaimFailure(request().post("/app/task/instance/1/claim"), ResultCode.UNAUTHORIZED);
        assertPending(1L);
        assertThat(activeMemberships(1L)).isZero();
    }

    /** 非法路径参数按原参数校验失败，不新增 membership。 */
    @Test
    void shouldRejectClaimWithInvalidInstanceId() {
        assertClaimFailure(authRequest(token, 1L).post("/app/task/instance/invalid/claim"), ResultCode.PARAM_ERROR);
        assertPending(1L);
        assertThat(activeMemberships(1L)).isZero();
    }

    /** 不存在的任务保持原业务错误，不能加入任何企划。 */
    @Test
    void shouldRejectClaimForMissingTask() {
        assertClaimFailure(claim(token, 999L), ResultCode.DATA_NOT_EXIT);
        assertPending(1L);
        assertThat(activeMemberships(1L)).isZero();
    }

    /** 非待接取任务保持原重复接取报错语义，不执行自动加入。 */
    @Test
    void shouldRejectClaimForAlreadyClaimedTaskWithoutJoining() {
        jdbcTemplate.update("UPDATE task_instance SET status=2, assignee_id=2 WHERE id=1");

        assertClaimFailure(claim(token, 1L), ResultCode.OPERATION_FAIL);

        assertThat(activeMemberships(1L)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM task_instance WHERE id=1", Long.class)).isEqualTo(2L);
    }

    /** 组织隔离不因自动加入被绕过，跨组织接取无状态或 membership 副作用。 */
    @Test
    void shouldRejectClaimFromAnotherOrganization() {
        createOrganization(2L, "另一个组织");
        createOrganizationMember(3L, 2L, 1L, "ORG_USER");

        assertClaimFailure(authRequest(token, 2L).post("/app/task/instance/1/claim"), ResultCode.UNAUTHORIZED_OPERATION);

        assertPending(1L);
        assertThat(activeMemberships(1L)).isZero();
    }

    /** 企划禁用或软删除时，接取与自动加入都必须拒绝。 */
    @ParameterizedTest
    @ValueSource(strings = {"disabled", "deleted"})
    void shouldRejectClaimForUnavailableProject(String state) {
        if (state.equals("disabled")) {
            jdbcTemplate.update("UPDATE project SET resource_status=0 WHERE id=1");
        } else {
            jdbcTemplate.update("UPDATE project SET deleted_at=NOW() WHERE id=1");
        }

        assertClaimFailure(claim(token, 1L), ResultCode.DATA_NOT_EXIT);

        assertPending(1L);
        assertThat(activeMemberships(1L)).isZero();
    }

    /** 项目软删除时保持原不可操作限制，不会误加入其企划。 */
    @Test
    void shouldRejectClaimForDeletedItem() {
        jdbcTemplate.update("UPDATE item SET deleted_at=NOW() WHERE id=1");

        assertClaimFailure(claim(token, 1L), ResultCode.DATA_NOT_EXIT);

        assertPending(1L);
        assertThat(activeMemberships(1L)).isZero();
    }

    /** 组织或成员禁用、成员移除时，组织鉴权仍在 claim 之前生效。 */
    @ParameterizedTest
    @ValueSource(strings = {"organizationDisabled", "memberDisabled", "memberDeleted"})
    void shouldRejectClaimWhenOrganizationAccessIsUnavailable(String state) {
        switch (state) {
            case "organizationDisabled" -> jdbcTemplate.update("UPDATE organization SET status=0 WHERE id=1");
            case "memberDisabled" -> jdbcTemplate.update("UPDATE organization_member SET status=0 WHERE id=1");
            case "memberDeleted" -> jdbcTemplate.update("UPDATE organization_member SET deleted_at=NOW() WHERE id=1");
            default -> throw new IllegalArgumentException(state);
        }

        assertClaimFailure(claim(token, 1L), ResultCode.OPERATION_FAIL);

        assertPending(1L);
        assertThat(activeMemberships(1L)).isZero();
    }

    /** 真实 INSERT 的非空约束失败时，前面的 CAS 接取必须一起回滚。 */
    @Test
    void shouldRollbackClaimWhenMembershipInsertFails() {
        doAnswer(call -> {
            ProjectWorkshop membership = call.getArgument(0, ProjectWorkshop.class);
            membership.setProjectId(null);
            return sqlSession.getMapper(ProjectWorkshopMapper.class).insert(membership);
        }).when(workshops).insert(any(ProjectWorkshop.class));

        assertClaimFailure(claim(token, 1L), ResultCode.FAIL);

        assertPending(1L);
        assertThat(membershipHistory(1L)).isZero();
    }

    /** 真实主键碰撞到其他用户的历史行时不能吞异常或误报已加入。 */
    @Test
    void shouldRollbackClaimWhenMembershipIdCollidesWithAnotherMembership() {
        createMembership(100L, 2L, true);
        doAnswer(call -> {
            ProjectWorkshop membership = call.getArgument(0, ProjectWorkshop.class);
            membership.setId(100L);
            return sqlSession.getMapper(ProjectWorkshopMapper.class).insert(membership);
        }).when(workshops).insert(any(ProjectWorkshop.class));

        assertClaimFailure(claim(token, 1L), ResultCode.FAIL);

        assertPending(1L);
        assertThat(membershipHistory(1L)).isZero();
        assertThat(membershipHistory(2L)).isEqualTo(1);
        assertThat(activeMemberships(2L)).isZero();
    }

    /** 两次真实写入后在事务提交前失败，HTTP 不得返回成功，任务和新增关联须整体回滚。 */
    @Test
    void shouldRollbackClaimAndMembershipWhenCommitFails() {
        doAnswer(call -> {
            int inserted = sqlSession.getMapper(ProjectWorkshopMapper.class)
                    .insert(call.getArgument(0, ProjectWorkshop.class));
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("forced failure before claim commit");
                }
            });
            return inserted;
        }).when(workshops).insert(any(ProjectWorkshop.class));

        assertClaimFailure(claim(token, 1L), ResultCode.FAIL);

        assertPending(1L);
        assertThat(membershipHistory(1L)).isZero();
    }

    private Response claim(String userToken, long instanceId) {
        return authRequest(userToken, 1L).post("/app/task/instance/" + instanceId + "/claim");
    }

    private void createTaskInstance(long instanceId) {
        jdbcTemplate.update("INSERT INTO task_instance "
                + "(id, project_id, item_id, item_task_node_id, task_flow_id, status) VALUES (?, 1, 1, 1, 1, 1)", instanceId);
    }

    private void createMembership(long id, long userId, boolean deleted) {
        jdbcTemplate.update("INSERT INTO project_workshop (id, project_id, user_id, deleted_at) VALUES (?, 1, ?, ?)",
                id, userId, deleted ? testNow().minusDays(1) : null);
    }

    private int activeMemberships(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_workshop WHERE project_id=1 AND user_id=? AND deleted_at IS NULL", Integer.class, userId);
    }

    private int membershipHistory(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_workshop WHERE project_id=1 AND user_id=?", Integer.class, userId);
    }

    private void assertClaimSuccess(Response response, boolean autoJoinedProject) {
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.autoJoinedProject", equalTo(autoJoinedProject));
    }

    private void assertClaimFailure(Response response, ResultCode resultCode) {
        response.then().statusCode(200).body("code", equalTo(resultCode.getCode()))
                .body("data.autoJoinedProject", nullValue());
    }

    private void assertClaimed(long instanceId, long userId) {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id=?", Integer.class, instanceId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM task_instance WHERE id=?", Long.class, instanceId)).isEqualTo(userId);
        assertThat(jdbcTemplate.queryForObject("SELECT claimed_at FROM task_instance WHERE id=?", java.sql.Timestamp.class, instanceId)).isNotNull();
    }

    private void assertPending(long instanceId) {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id=?", Integer.class, instanceId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM task_instance WHERE id=?", Long.class, instanceId)).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT claimed_at FROM task_instance WHERE id=?", java.sql.Timestamp.class, instanceId)).isNull();
    }

    private void synchronizeTaskSnapshots(int parties) {
        CyclicBarrier afterSnapshot = new CyclicBarrier(parties);
        AtomicInteger reads = new AtomicInteger();
        doAnswer(call -> {
            TaskInstance snapshot = sqlSession.getMapper(TaskInstanceMapper.class)
                    .selectById(call.getArgument(0, Serializable.class));
            // 只同步锁前定位读取；锁内重读不能等待其他尚未取得锁的请求。
            if (reads.incrementAndGet() <= parties) {
                afterSnapshot.await(15, TimeUnit.SECONDS);
            }
            return snapshot;
        }).when(instances).selectById(any(Serializable.class));
    }

    private List<Response> concurrently(List<Supplier<Response>> requests) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Response>> futures = new ArrayList<>();
            for (Supplier<Response> request : requests) {
                futures.add(executor.submit(request::get));
            }
            List<Response> responses = new ArrayList<>();
            for (Future<Response> future : futures) {
                responses.add(future.get(30, TimeUnit.SECONDS));
            }
            return responses;
        }
    }
}
