package online.longlian.app.api.app;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.service.app.impl.UserOperationLogService;
import online.longlian.app.service.common.LockService;
import online.longlian.common.enumeration.UserOperationType;
import online.longlian.common.service.DistributedLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.AopTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/** API assertions deliberately read committed rows, including the audit and membership side effects. */
public class TaskOwnerReviewApiTest extends BaseApiTest {

    @SpyBean
    private LockService lockService;

    @SpyBean
    private UserOperationLogService operationLogService;

    @Test
    void shouldCancelClaimWithoutDeletingHistoryOrMembership() {
        Tokens tokens = createWorkflow();
        insertHistory(101, 1, 2, "earlier rejection");
        insertHistory(102, 2, 3, "earlier reset");
        List<Map<String, Object>> history = submissions();
        List<Map<String, Object>> members = members();
        List<Map<String, Object>> workshops = workshops();
        assertRevision(tokens.owner(), 1, "0");

        assertCode(operate(tokens.owner(), "abandon", "0", "  调整接取人 \n"), ResultCode.SUCCESS);

        assertInstance(1, 1, null, 1);
        assertThat(instance(1).get("claimed_at")).isNull();
        assertThat(instance(1).get("submitted_at")).isNull();
        assertThat(instance(1).get("completed_at")).isNull();
        assertThat(submissions()).isEqualTo(history);
        assertThat(members()).isEqualTo(members);
        assertThat(workshops()).isEqualTo(workshops);
        assertRevision(tokens.owner(), 1, "1");
        assertAudit("OWNER_CANCEL", "调整接取人", "0", "1", 3);
    }

    @Test
    void shouldRejectOnlyCurrentSubmissionAndDecrementOnceWhilePreservingAssignee() {
        Tokens tokens = createWorkflow();
        insertHistory(101, 1, 2, "old rejection");
        insertHistory(102, 2, 3, "old reset");
        List<Map<String, Object>> historical = submissions();
        Object claimedAt = instance(1).get("claimed_at");
        assertCode(submit(tokens.worker(), 1), ResultCode.SUCCESS);
        assertInstance(1, 3, 2L, 1);
        assertCount(5);
        List<Map<String, Object>> workshops = workshops();

        assertCode(operate(tokens.owner(), "reject", "1", "  第三页需要修改  "), ResultCode.SUCCESS);

        assertInstance(1, 2, 2L, 2);
        assertThat(instance(1).get("claimed_at")).isEqualTo(claimedAt);
        assertThat(instance(1).get("submitted_at")).isNull();
        assertThat(instance(1).get("completed_at")).isNull();
        assertThat(submissions().subList(0, 2)).isEqualTo(historical);
        Map<String, Object> current = submissions().get(2);
        assertThat(((Number) current.get("status")).intValue()).isEqualTo(2);
        assertThat(((Number) current.get("reviewer_id")).longValue()).isEqualTo(1L);
        assertThat(current.get("reviewed_at")).isNotNull();
        assertThat(current.get("review_comment")).isEqualTo("第三页需要修改");
        assertCount(4);
        assertThat(workshops()).isEqualTo(workshops);
        assertThat(members()).hasSize(2);
        assertAudit("OWNER_REJECT", "第三页需要修改", "1", "2", 4);
        assertRevision(tokens.owner(), 1, "2");
        authRequest(tokens.owner()).get("/app/task/instance/1/submissions").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", hasSize(3))
                .body("data[0].status", equalTo("REJECTED"))
                .body("data[0].reviewer.id", equalTo("1"))
                .body("data[0].reviewComment", equalTo("第三页需要修改"))
                .body("data[0].reviewedAt", notNullValue());

        Snapshot rejected = snapshot();
        assertCode(operate(tokens.owner(), "reject", "1", "重复"), ResultCode.OPERATION_FAIL);
        assertCode(operate(tokens.owner(), "reject", "2", "状态已改变"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(rejected);
    }

    @Test
    void shouldPreserveAutoJoinedMembershipWhenOwnerCancelsClaim() {
        Tokens tokens = createWorkflow();
        jdbcTemplate.update("DELETE FROM project_workshop WHERE project_id = 1 AND user_id = 2");
        assertCode(operate(tokens.owner(), "abandon", "0", "重新分配"), ResultCode.SUCCESS);

        authRequest(tokens.worker()).post("/app/task/instance/1/claim").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.autoJoinedProject", equalTo(true));
        List<Map<String, Object>> autoJoined = workshops();
        assertThat(autoJoined).hasSize(2);

        assertCode(operate(tokens.owner(), "abandon", "2", "保留企划成员"), ResultCode.SUCCESS);

        assertInstance(1, 1, null, 3);
        assertThat(workshops()).isEqualTo(autoJoined);
        assertCount(4);
    }

    @Test
    void shouldRejectStaleCancelAfterClaimAbandonClaimAbaWithoutSideEffects() {
        Tokens tokens = createWorkflow();
        assertRevision(tokens.owner(), 1, "0");
        assertCode(operate(tokens.owner(), "abandon", "0", "取消"), ResultCode.SUCCESS);
        assertCode(authRequest(tokens.worker()).post("/app/task/instance/1/claim"), ResultCode.SUCCESS);
        assertCode(authRequest(tokens.worker()).body(Map.of("expectedRevision", "2"))
                .post("/app/task/instance/1/abandon"), ResultCode.SUCCESS);
        assertCode(authRequest(tokens.worker()).post("/app/task/instance/1/claim"), ResultCode.SUCCESS);
        assertInstance(1, 2, 2L, 4);
        assertRevision(tokens.owner(), 1, "4");
        Snapshot before = snapshot();

        assertCode(operate(tokens.owner(), "abandon", "0", "过期请求"), ResultCode.OPERATION_FAIL);
        assertCode(operate(tokens.owner(), "abandon", "2", "旧接取版本"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldRejectStaleRejectAfterResubmissionAbaWithoutChangingNewVersion() {
        Tokens tokens = createWorkflow();
        assertCode(submit(tokens.worker(), 1), ResultCode.SUCCESS);
        assertCode(operate(tokens.owner(), "reject", "1", "第一次"), ResultCode.SUCCESS);
        assertCode(submit(tokens.worker(), 1), ResultCode.SUCCESS);
        assertInstance(1, 3, 2L, 3);
        Snapshot before = snapshot();

        assertCode(operate(tokens.owner(), "reject", "1", "旧完成版本"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(before);
        assertCode(operate(tokens.owner(), "reject", "3", "第二次"), ResultCode.SUCCESS);
        assertInstance(1, 2, 2L, 4);
        assertCount(4);
        assertThat(submissions()).hasSize(2);
    }

    @Test
    void shouldIncrementRevisionForResetAndAllowOwnerRejectWithoutNextStageAssignment() {
        Tokens tokens = createWorkflow();
        assertCode(submit(tokens.worker(), 1), ResultCode.SUCCESS);
        assertCode(authRequest(tokens.worker()).post("/app/task/instance/1/reset"), ResultCode.SUCCESS);
        assertInstance(1, 2, 2L, 2);
        assertCode(submit(tokens.worker(), 1), ResultCode.SUCCESS);
        assertCode(operate(tokens.owner(), "reject", "3", "负责人审核"), ResultCode.SUCCESS);
        assertInstance(1, 2, 2L, 4);
        assertCount(4);
        assertRevision(tokens.owner(), 1, "4");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldAllowAssigneeToAbandonWithoutReasonEvenWhenAlsoProjectCreator(boolean projectCreator) {
        Tokens tokens = createWorkflow();
        if (projectCreator) {
            jdbcTemplate.update("UPDATE project SET creator_id = 2 WHERE id = 1");
        }
        assertCode(authRequest(tokens.worker()).body(Map.of("expectedRevision", "0"))
                .post(path("abandon")), ResultCode.SUCCESS);
        assertInstance(1, 1, null, 1);
        assertCount(4);
        assertThat(ownerAudits()).isEmpty();
        assertThat(workshops()).hasSize(2);
    }

    @Test
    void shouldRecordReasonWhenProjectCreatorAbandonsOwnClaim() {
        Tokens tokens = createWorkflow();
        jdbcTemplate.update("UPDATE task_instance SET assignee_id = 1 WHERE id = 1");
        assertCode(operate(tokens.owner(), "abandon", "0", "  主动调整  "), ResultCode.SUCCESS);
        assertInstance(1, 1, null, 1);
        JSONObject audit = JSON.parseObject(ownerAudits().getFirst().get("request_body").toString());
        assertThat(audit.getString("action")).isEqualTo("OWNER_CANCEL");
        assertThat(audit.getString("reason")).isEqualTo("主动调整");
        assertThat(audit.getLong("assigneeId")).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldApplyRevisionAndCompletedDownstreamProtectionToAssigneeAndNextStageExecutor(String action) {
        Tokens tokens = createReadyWorkflow(action);
        if (action.equals("reject")) {
            insertSlot(2, 2, 1, 2, 2L);
        }
        Snapshot before = snapshot();
        assertCode(operate(tokens.worker(), action, "99", "过期操作"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(before);
        insertSlot(3, 4, 1, 3, 2L);
        Snapshot downstreamCompleted = snapshot();
        assertCode(operate(tokens.worker(), action, revision(), "下游保护"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(downstreamCompleted);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldApplyOwnershipValidationToAssigneeAndNextStageExecutor(String action) {
        Tokens tokens = createReadyWorkflow(action);
        if (action.equals("reject")) {
            insertSlot(2, 2, 1, 2, 2L);
        }
        jdbcTemplate.update("UPDATE item_task_flow SET project_id = 999 WHERE id = 1");
        Snapshot before = snapshot();
        assertCode(operate(tokens.worker(), action, revision(), "不能绕过归属"), ResultCode.UNAUTHORIZED_OPERATION);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldRejectCurrentVersionThroughNextStageExecutorAndPreserveReviewMetadata() {
        Tokens tokens = createReadyWorkflow("reject");
        insertSlot(2, 2, 1, 2, 2L);
        insertHistory(101, 2, 2, "保留旧版本");
        assertCode(operate(tokens.worker(), "reject", "1", "  下一阶段审核  "), ResultCode.SUCCESS);
        assertInstance(1, 2, 2L, 2);
        assertInstance(2, 2, 2L, 0);
        assertCount(4);
        assertThat(jdbcTemplate.queryForObject("SELECT review_comment FROM task_submission WHERE version_no = 1", String.class))
                .isEqualTo("下一阶段审核");
        assertThat(jdbcTemplate.queryForObject("SELECT reviewer_id FROM task_submission WHERE version_no = 1", Long.class))
                .isEqualTo(2L);
        assertThat(submissions().get(1).get("review_comment")).isEqualTo("保留旧版本");
        Snapshot rejected = snapshot();
        assertCode(operate(tokens.worker(), "reject", "1", "重复请求"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(rejected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldDenyUnauthenticatedAndNonOwnerIncludingOrganizationAdmin(String action) {
        Tokens tokens = createReadyWorkflow(action);
        createTestUser(3L, "unrelated", "123456", "unrelated@example.com");
        createOrganizationMember(3L, 1L, 3L, "ORG_USER");
        String unrelated = loginAs("unrelated", "123456");
        Snapshot before = snapshot();
        assertCode(request().body(body(action, revision(), "理由")).post(path(action)), ResultCode.UNAUTHORIZED);
        assertCode(operate(unrelated, action, revision(), "非负责人或执行人"), ResultCode.UNAUTHORIZED_OPERATION);
        jdbcTemplate.update("UPDATE organization_member SET org_role = 'ORG_ADMIN' WHERE id = 3");
        Snapshot promoted = snapshot();
        assertCode(operate(unrelated, action, revision(), "管理员也无权"), ResultCode.UNAUTHORIZED_OPERATION);
        assertThat(snapshot()).isEqualTo(promoted);
        assertThat(instances()).isEqualTo(before.instances());
        assertThat(submissions()).isEqualTo(before.submissions());
        assertThat(ownerAudits()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldDenyCrossOrganizationEvenWhenCallerOwnsProjectAndBothOrganizations(String action) {
        Tokens tokens = createReadyWorkflow(action);
        createOrganization(2L, "另一组织");
        createOrganizationMember(3L, 2L, 1L, "ORG_OWNER");
        Snapshot before = snapshot();

        assertCode(authRequest(tokens.owner(), 2L).body(body(action, revision(), "跨组织"))
                .post(path(action)), ResultCode.UNAUTHORIZED_OPERATION);

        assertThat(snapshot()).isEqualTo(before);
    }

    static Stream<Arguments> inactiveMemberships() {
        return Stream.of("abandon", "reject").flatMap(action -> Stream.of(
                "UPDATE organization_member SET status = 0 WHERE id = 1",
                "UPDATE organization_member SET deleted_at = NOW() WHERE id = 1",
                "DELETE FROM organization_member WHERE id = 1")
                .map(sql -> Arguments.of(action, sql)));
    }

    @ParameterizedTest
    @MethodSource("inactiveMemberships")
    void shouldDenyOwnerWithoutCurrentEnabledOrganizationMembership(String action, String sql) {
        Tokens tokens = createReadyWorkflow(action);
        jdbcTemplate.update(sql);
        Snapshot before = snapshot();
        assertCode(operate(tokens.owner(), action, revision(), "无效成员"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(before);
    }

    static Stream<Arguments> corruptedOwnerships() {
        return Stream.of("abandon", "reject").flatMap(action -> Stream.of(
                "UPDATE task_instance SET project_id = 2 WHERE id = 1",
                "UPDATE task_instance SET item_id = 2 WHERE id = 1",
                "UPDATE task_instance SET item_task_node_id = 2 WHERE id = 1",
                "UPDATE task_instance SET task_flow_id = 2 WHERE id = 1",
                "UPDATE item SET project_id = 2 WHERE id = 1",
                "UPDATE item_task_node SET item_id = 2 WHERE id = 1",
                "UPDATE item_task_node SET project_id = 2 WHERE id = 1",
                "UPDATE item_task_node SET item_task_flow_id = 2 WHERE id = 1",
                "UPDATE item_task_flow SET item_id = 999 WHERE id = 1",
                "UPDATE item_task_flow SET project_id = 2 WHERE id = 1",
                "DELETE FROM item_task_node WHERE id = 1",
                "DELETE FROM item_task_flow WHERE id = 1",
                "UPDATE item_task_node SET deleted_at = NOW() WHERE id = 1",
                "UPDATE item_task_flow SET deleted_at = NOW() WHERE id = 1")
                .map(sql -> Arguments.of(action, sql)));
    }

    @ParameterizedTest
    @MethodSource("corruptedOwnerships")
    void shouldDenyCrossProjectAndCorruptNodeOrFlowOwnership(String action, String sql) {
        Tokens tokens = createReadyWorkflow(action);
        createOtherProject();
        jdbcTemplate.update(sql);
        Snapshot before = snapshot();
        assertCode(operate(tokens.owner(), action, revision(), "归属损坏"), ResultCode.UNAUTHORIZED_OPERATION);
        assertThat(snapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldDenyMissingInstanceWithoutWritingAudit(String action) {
        Tokens tokens = createReadyWorkflow(action);
        Snapshot before = snapshot();
        assertCode(authRequest(tokens.owner()).body(body(action, revision(), "不存在的任务"))
                .post("/app/task/instance/999/" + action), ResultCode.DATA_NOT_EXIT);
        assertThat(snapshot()).isEqualTo(before);
    }

    static Stream<Arguments> missingParents() {
        return Stream.of("abandon", "reject").flatMap(action -> Stream.of(
                "DELETE FROM item WHERE id = 1", "DELETE FROM project WHERE id = 1",
                "UPDATE project SET resource_status = 0 WHERE id = 1")
                .map(sql -> Arguments.of(action, sql)));
    }

    @ParameterizedTest
    @MethodSource("missingParents")
    void shouldDenyMissingItemOrUnavailableProjectWithoutSideEffects(String action, String sql) {
        Tokens tokens = createReadyWorkflow(action);
        jdbcTemplate.update(sql);
        Snapshot before = snapshot();
        assertCode(operate(tokens.owner(), action, revision(), "不可用归属"), ResultCode.DATA_NOT_EXIT);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldPreserveRevisionPrecisionBeyondJavascriptSafeInteger() {
        Tokens tokens = createWorkflow();
        jdbcTemplate.update("UPDATE task_instance SET revision = 9007199254740993 WHERE id = 1");
        assertRevision(tokens.owner(), 1, "9007199254740993");
        assertCode(operate(tokens.owner(), "abandon", "9007199254740993", "精确修订号"), ResultCode.SUCCESS);
        assertInstance(1, 1, null, 9007199254740994L);
        assertRevision(tokens.owner(), 1, "9007199254740994");
        assertAudit("OWNER_CANCEL", "精确修订号", "9007199254740993", "9007199254740994", 3);
    }

    static Stream<Arguments> invalidStates() {
        return Stream.of(Arguments.of("abandon", 1), Arguments.of("abandon", 3),
                Arguments.of("reject", 1), Arguments.of("reject", 2));
    }

    @ParameterizedTest
    @MethodSource("invalidStates")
    void shouldRejectInvalidSourceStateWithoutSideEffects(String action, int status) {
        Tokens tokens = createWorkflow();
        jdbcTemplate.update("UPDATE task_instance SET status = ? WHERE id = 1", status);
        Snapshot before = snapshot();
        assertCode(operate(tokens.owner(), action, "0", "非法状态"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(before);
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of("abandon", "reject").flatMap(action -> Stream.of(
                "{}", "{\"reason\":\"理由\"}", "{\"expectedRevision\":null,\"reason\":\"理由\"}",
                "{\"expectedRevision\":\"\",\"reason\":\"理由\"}",
                "{\"expectedRevision\":\"-1\",\"reason\":\"理由\"}",
                "{\"expectedRevision\":\"+1\",\"reason\":\"理由\"}",
                "{\"expectedRevision\":\"01\",\"reason\":\"理由\"}",
                "{\"expectedRevision\":\" 0 \",\"reason\":\"理由\"}",
                "{\"expectedRevision\":\"1e0\",\"reason\":\"理由\"}",
                "{\"expectedRevision\":\"1.0\",\"reason\":\"理由\"}",
                "{\"expectedRevision\":\"abc\",\"reason\":\"理由\"}",
                "{\"expectedRevision\":\"9223372036854775808\",\"reason\":\"理由\"}",
                "{\"expectedRevision\":\"0\"}", "{\"expectedRevision\":\"0\",\"reason\":null}",
                "{\"expectedRevision\":\"0\",\"reason\":\"\"}",
                "{\"expectedRevision\":\"0\",\"reason\":\"  \\n\\t \"}",
                JSON.toJSONString(Map.of("expectedRevision", "0", "reason", "字".repeat(501))))
                .map(json -> Arguments.of(action, action.equals("reject")
                        ? json.replace("\"reason\"", "\"reviewComment\"") : json)));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void shouldRejectMissingMalformedOrOversizedParametersWithoutSideEffects(String action, String json) {
        Tokens tokens = createReadyWorkflow(action);
        Snapshot before = snapshot();
        assertCode(authRequest(tokens.owner()).body(json).post(path(action)), ResultCode.PARAM_ERROR);
        assertThat(snapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 500})
    void shouldAcceptTrimmedReasonBoundariesForBothOwnerOperations(int length) {
        Tokens tokens = createWorkflow();
        String reason = "字".repeat(length);
        assertCode(operate(tokens.owner(), "abandon", "0", " \t" + reason + "\n "), ResultCode.SUCCESS);
        assertAudit("OWNER_CANCEL", reason, "0", "1", 3);
        assertCode(authRequest(tokens.worker()).post("/app/task/instance/1/claim"), ResultCode.SUCCESS);
        assertCode(submit(tokens.worker(), 1), ResultCode.SUCCESS);
        assertCode(operate(tokens.owner(), "reject", "3", " \t" + reason + "\n "), ResultCode.SUCCESS);
        assertThat(submissions().getFirst().get("review_comment")).isEqualTo(reason);
        assertAudit("OWNER_REJECT", reason, "3", "4", 4);
        assertInstance(1, 2, 2L, 4);
        assertCount(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldTreatLongMaxRevisionAsValidButStaleRatherThanMalformed(String action) {
        Tokens tokens = createReadyWorkflow(action);
        Snapshot before = snapshot();
        assertCode(operate(tokens.owner(), action, Long.toString(Long.MAX_VALUE), "合法long但已过期"),
                ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldOperateOnlyTargetParallelSlotAndPreserveClaimedDownstream(String action) {
        Tokens tokens = createReadyWorkflow(action);
        insertSlot(2, 1, 2, 3, 2L);
        insertSlot(3, 1, 3, 1, null);
        insertSlot(4, 2, 1, 2, 2L);
        insertSlot(5, 3, 1, 2, 2L);
        Map<String, Object> sibling = instance(2);
        Map<String, Object> pendingSibling = instance(3);
        Map<String, Object> downstream = instance(4);
        Map<String, Object> laterDownstream = instance(5);

        assertCode(operate(tokens.owner(), action, revision(), "仅处理目标槽"), ResultCode.SUCCESS);

        assertThat(instance(2)).isEqualTo(sibling);
        assertThat(instance(3)).isEqualTo(pendingSibling);
        assertThat(instance(4)).isEqualTo(downstream);
        assertThat(instance(5)).isEqualTo(laterDownstream);
        Snapshot beforeSubmit = snapshot();
        assertCode(submit(tokens.worker(), 4), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(beforeSubmit);
    }

    static Stream<Arguments> completedDownstream() {
        return Stream.of("abandon", "reject").flatMap(action -> Stream.of(2, 4)
                .map(sort -> Arguments.of(action, sort)));
    }

    @ParameterizedTest
    @MethodSource("completedDownstream")
    void shouldRejectWhenAnyLaterSortCompletedEvenWithClaimedIntermediateStage(String action, int completedSort) {
        Tokens tokens = createReadyWorkflow(action);
        insertSlot(2, 2, 1, 2, 2L);
        insertSlot(3, completedSort, 2, 3, 2L);
        Snapshot before = snapshot();
        assertCode(operate(tokens.owner(), action, revision(), "下游已经完成"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldIgnoreCompletedSlotsInOtherItems() {
        Tokens tokens = createWorkflow();
        createOtherProject();
        jdbcTemplate.update("INSERT INTO task_instance "
                + "(id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status) "
                + "VALUES (2, 2, 2, 2, 2, 2, 3)");
        Map<String, Object> other = instance(2);
        assertCode(operate(tokens.owner(), "abandon", "0", "其他项目不影响"), ResultCode.SUCCESS);
        assertThat(instance(2)).isEqualTo(other);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void shouldRollbackRejectWhenCurrentSubmittedVersionIsNotUnique(int currentVersions) {
        Tokens tokens = createWorkflow();
        jdbcTemplate.update("UPDATE task_instance SET status = 3, submitted_at = NOW(), completed_at = NOW() WHERE id = 1");
        for (int i = 0; i < currentVersions; i++) {
            insertHistory(101 + i, i + 1, 1, "");
        }
        insertSlot(2, 2, 1, 2, 2L);
        Snapshot before = snapshot();
        assertCode(operate(tokens.owner(), "reject", "0", "当前版本损坏"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(before);
        assertCode(operate(tokens.worker(), "reject", "0", "下一阶段也不能绕过版本保护"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void shouldDecrementActualSubmitterCountEvenAfterSubmitterIsDisabled() {
        Tokens tokens = createReadyWorkflow("reject");
        jdbcTemplate.update("UPDATE organization_member SET status = 0 WHERE id = 2");
        assertCode(operate(tokens.owner(), "reject", "1", "提交人已停用"), ResultCode.SUCCESS);
        assertCount(4);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM organization_member WHERE id = 2", Integer.class))
                .isZero();
        assertThat(workshops()).hasSize(2);
        assertInstance(1, 2, 2L, 2);
    }

    @Test
    void shouldDecrementCurrentSubmissionSubmitterRatherThanLegacyAssignee() {
        Tokens tokens = createReadyWorkflow("reject");
        jdbcTemplate.update("UPDATE organization_member SET submit_count = 7 WHERE id = 1");
        jdbcTemplate.update("UPDATE task_submission SET submitter_id = 1 WHERE task_instance_id = 1");

        assertCode(operate(tokens.owner(), "reject", "1", "历史接取人与提交人不一致"), ResultCode.SUCCESS);

        assertThat(jdbcTemplate.queryForObject("SELECT submit_count FROM organization_member WHERE id = 1", Integer.class))
                .isEqualTo(6);
        assertCount(5);
        assertInstance(1, 2, 2L, 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldRollbackEntireOwnerOperationWhenSynchronousAuditFails(String action) {
        Tokens tokens = createReadyWorkflow(action);
        Snapshot before = snapshot();
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            invocation.callRealMethod();
            throw new IllegalStateException("forced audit persistence failure");
        }).when(AopTestUtils.<UserOperationLogService>getUltimateTargetObject(operationLogService))
                .logSynchronously(eq(1L), eq(1L), eq(1L), any(UserOperationType.class), any());

        assertCode(operate(tokens.owner(), action, revision(), "审计必须原子提交"), ResultCode.FAIL);

        assertThat(snapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"claim", "abandon", "submit", "reset", "reject"})
    void shouldCommitEachMutationThroughSameItemWatchdogLock(String action) {
        Tokens tokens = createWorkflow();
        if (action.equals("claim")) {
            jdbcTemplate.update("UPDATE task_instance SET status = 1, assignee_id = NULL, claimed_at = NULL WHERE id = 1");
        }
        if (List.of("reset", "reject").contains(action)) {
            assertCode(submit(tokens.worker(), 1), ResultCode.SUCCESS);
        }
        long beforeRevision = Long.parseLong(revision());
        AtomicInteger lockEntries = new AtomicInteger();
        AtomicBoolean committedBeforeUnlock = new AtomicBoolean();
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            lockEntries.incrementAndGet();
            DistributedLockService.Lock acquired = (DistributedLockService.Lock) invocation.callRealMethod();
            DistributedLockService.Lock observed = spy(acquired);
            doAnswer(close -> {
                try {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    assertThat(Long.parseLong(revision())).isEqualTo(beforeRevision + 1);
                    committedBeforeUnlock.set(true);
                    return close.callRealMethod();
                } finally {
                    if (!committedBeforeUnlock.get()) {
                        acquired.close();
                    }
                }
            }).when(observed).close();
            return observed;
        }).when(lockService).tryAcquireOrThrow(eq("task:item:1"), anyLong(), eq(TimeUnit.SECONDS));

        Response response = switch (action) {
            case "abandon", "reject" -> operate(tokens.owner(), action, Long.toString(beforeRevision), "统一锁");
            case "submit" -> submit(tokens.worker(), 1);
            default -> authRequest(tokens.worker()).post(path(action));
        };
        assertCode(response, ResultCode.SUCCESS);
        assertThat(lockEntries).hasValue(1);
        assertThat(committedBeforeUnlock).isTrue();
        int expectedStatus = switch (action) {
            case "abandon" -> 1;
            case "submit" -> 3;
            default -> 2;
        };
        assertInstance(1, expectedStatus, expectedStatus == 1 ? null : 2L, beforeRevision + 1);
        assertCount(action.equals("submit") ? 5 : 4);
    }

    /** Pause before acquiring the lock, then let submit commit: the owner request must re-read under the lock. */
    @Test
    void shouldRejectStaleOwnerCancelAfterConcurrentSubmitCommits() throws Exception {
        Tokens tokens = createWorkflow();
        CountDownLatch beforeLock = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstLock(beforeLock, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> owner = CompletableFuture.supplyAsync(() ->
                    operate(tokens.owner(), "abandon", "0", "旧请求"), executor);
            Snapshot submitted;
            try {
                await(beforeLock);
                assertCode(submit(tokens.worker(), 1), ResultCode.SUCCESS);
                submitted = snapshot();
            } finally {
                resume.countDown();
            }
            assertCode(owner.get(15, TimeUnit.SECONDS), ResultCode.OPERATION_FAIL);
            assertThat(snapshot()).isEqualTo(submitted);
        }
        assertInstance(1, 3, 2L, 1);
        assertCount(5);
    }

    @Test
    void shouldRejectConcurrentSubmitAfterOwnerCancelCommits() throws Exception {
        Tokens tokens = createWorkflow();
        CountDownLatch beforeLock = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstLock(beforeLock, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> submit = CompletableFuture.supplyAsync(() -> submit(tokens.worker(), 1), executor);
            Snapshot cancelled;
            try {
                await(beforeLock);
                assertCode(operate(tokens.owner(), "abandon", "0", "先取消"), ResultCode.SUCCESS);
                cancelled = snapshot();
            } finally {
                resume.countDown();
            }
            assertCode(submit.get(15, TimeUnit.SECONDS), ResultCode.OPERATION_FAIL);
            assertThat(snapshot()).isEqualTo(cancelled);
        }
        assertInstance(1, 1, null, 1);
        assertCount(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldCommitExactlyOnceForConcurrentDuplicateOwnerRequests(String action) throws Exception {
        Tokens tokens = createReadyWorkflow(action);
        String expectedRevision = revision();
        Snapshot before = snapshot();
        CountDownLatch auditWritten = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicBoolean closedAfterCommit = new AtomicBoolean();
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            DistributedLockService.Lock acquired = (DistributedLockService.Lock) invocation.callRealMethod();
            DistributedLockService.Lock observed = spy(acquired);
            doAnswer(close -> {
                try {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    assertThat(ownerAudits()).hasSize(1);
                    closedAfterCommit.set(true);
                    return close.callRealMethod();
                } finally {
                    if (!closedAfterCommit.get()) {
                        acquired.close();
                    }
                }
            }).when(observed).close();
            return observed;
        }).when(lockService).tryAcquireOrThrow(eq("task:item:1"), anyLong(), eq(TimeUnit.SECONDS));
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            Object result = invocation.callRealMethod();
            auditWritten.countDown();
            await(resume);
            return result;
        }).when(AopTestUtils.<UserOperationLogService>getUltimateTargetObject(operationLogService))
                .logSynchronously(eq(1L), eq(1L), eq(1L), any(UserOperationType.class), any());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> first = CompletableFuture.supplyAsync(() ->
                    operate(tokens.owner(), action, expectedRevision, "并发操作"), executor);
            try {
                await(auditWritten);
                // The audit has been inserted but no mutation may be visible before transaction commit.
                assertThat(snapshot()).isEqualTo(before);
                CompletableFuture<Response> duplicate = CompletableFuture.supplyAsync(() ->
                        operate(tokens.owner(), action, expectedRevision, "重复并发"), executor);
                CompletableFuture<Response> concurrentSubmit = CompletableFuture.supplyAsync(() ->
                        submit(tokens.worker(), 1), executor);
                assertCode(duplicate.get(15, TimeUnit.SECONDS), ResultCode.OPERATION_FAIL);
                assertCode(concurrentSubmit.get(15, TimeUnit.SECONDS), ResultCode.OPERATION_FAIL);
                assertThat(snapshot()).isEqualTo(before);
            } finally {
                resume.countDown();
            }
            assertCode(first.get(15, TimeUnit.SECONDS), ResultCode.SUCCESS);
        }
        assertThat(closedAfterCommit).isTrue();
        assertThat(ownerAudits()).hasSize(1);
        assertInstance(1, action.equals("abandon") ? 1 : 2,
                action.equals("abandon") ? null : 2L, Long.parseLong(expectedRevision) + 1);
        assertCount(4);
        Snapshot committed = snapshot();
        assertCode(operate(tokens.owner(), action, expectedRevision, "完成后重复"), ResultCode.OPERATION_FAIL);
        assertThat(snapshot()).isEqualTo(committed);
    }

    @Test
    void shouldRejectDownstreamSubmitAfterConcurrentUpstreamOwnerRejectCommits() throws Exception {
        Tokens tokens = createReadyWorkflow("reject");
        insertSlot(2, 2, 1, 2, 2L);
        CountDownLatch beforeLock = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstLock(beforeLock, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> downstream = CompletableFuture.supplyAsync(() -> submit(tokens.worker(), 2), executor);
            Snapshot rejected;
            try {
                await(beforeLock);
                assertCode(operate(tokens.owner(), "reject", "1", "先打回上游"), ResultCode.SUCCESS);
                rejected = snapshot();
            } finally {
                resume.countDown();
            }
            assertCode(downstream.get(15, TimeUnit.SECONDS), ResultCode.OPERATION_FAIL);
            assertThat(snapshot()).isEqualTo(rejected);
        }
        assertInstance(1, 2, 2L, 2);
        assertInstance(2, 2, 2L, 0);
        assertCount(4);
        assertAudit("OWNER_REJECT", "先打回上游", "1", "2", 4);
    }

    @Test
    void shouldRejectUpstreamOwnerRejectAfterConcurrentDownstreamSubmitCommits() throws Exception {
        Tokens tokens = createReadyWorkflow("reject");
        insertSlot(2, 2, 1, 2, 2L);
        CountDownLatch beforeLock = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstLock(beforeLock, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> upstream = CompletableFuture.supplyAsync(() ->
                    operate(tokens.owner(), "reject", "1", "不能打回已完成下游的上游"), executor);
            Snapshot completed;
            try {
                await(beforeLock);
                assertCode(submit(tokens.worker(), 2), ResultCode.SUCCESS);
                completed = snapshot();
            } finally {
                resume.countDown();
            }
            assertCode(upstream.get(15, TimeUnit.SECONDS), ResultCode.OPERATION_FAIL);
            assertThat(snapshot()).isEqualTo(completed);
        }
        assertInstance(1, 3, 2L, 1);
        assertInstance(2, 3, 2L, 1);
        assertCount(6);
        assertThat(ownerAudits()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"membership", "creator"})
    void shouldRecheckOwnerAndEnabledMembershipInsideItemLock(String changedIdentity) throws Exception {
        Tokens tokens = createWorkflow();
        CountDownLatch beforeLock = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstLock(beforeLock, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> pending = CompletableFuture.supplyAsync(() ->
                    operate(tokens.owner(), "abandon", "0", "旧成员身份"), executor);
            Snapshot disabled;
            try {
                await(beforeLock);
                jdbcTemplate.update(changedIdentity.equals("membership")
                        ? "UPDATE organization_member SET status = 0 WHERE id = 1"
                        : "UPDATE project SET creator_id = 2 WHERE id = 1");
                disabled = snapshot();
            } finally {
                resume.countDown();
            }
            assertCode(pending.get(15, TimeUnit.SECONDS), changedIdentity.equals("membership")
                    ? ResultCode.OPERATION_FAIL : ResultCode.UNAUTHORIZED_OPERATION);
            assertThat(snapshot()).isEqualTo(disabled);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldRejectWhenInstanceDisappearsBeforeLockAcquisition(String action) throws Exception {
        Tokens tokens = createReadyWorkflow(action);
        String expectedRevision = revision();
        CountDownLatch beforeLock = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstLock(beforeLock, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> pending = CompletableFuture.supplyAsync(() ->
                    operate(tokens.owner(), action, expectedRevision, "旧目标"), executor);
            Snapshot removed;
            try {
                await(beforeLock);
                jdbcTemplate.update("DELETE FROM task_instance WHERE id = 1");
                removed = snapshot();
            } finally {
                resume.countDown();
            }
            assertCode(pending.get(15, TimeUnit.SECONDS), ResultCode.DATA_NOT_EXIT);
            assertThat(snapshot()).isEqualTo(removed);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "reject"})
    void shouldRejectWhenInstanceMovesToAnotherValidItemBeforeLockAcquisition(String action) throws Exception {
        Tokens tokens = createReadyWorkflow(action);
        String expectedRevision = revision();
        CountDownLatch beforeLock = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstLock(beforeLock, resume);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> pending = CompletableFuture.supplyAsync(() ->
                    operate(tokens.owner(), action, expectedRevision, "旧项目目标"), executor);
            Snapshot relocated;
            try {
                await(beforeLock);
                jdbcTemplate.update("INSERT INTO item (id, project_id, title, task_template_id, creator_id) "
                        + "VALUES (2, 1, '迁入项目', 0, 1)");
                jdbcTemplate.update("INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) "
                        + "VALUES (2, 2, 1, 1, '迁入任务流')");
                jdbcTemplate.update("INSERT INTO item_task_node "
                        + "(id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                        + "VALUES (2, 2, 2, 1, 1, '迁入节点', '[]', 1, 1)");
                jdbcTemplate.update("UPDATE task_instance SET item_id = 2, item_task_node_id = 2, task_flow_id = 2 WHERE id = 1");
                relocated = snapshot();
            } finally {
                resume.countDown();
            }
            assertCode(pending.get(15, TimeUnit.SECONDS), ResultCode.OPERATION_FAIL);
            assertThat(snapshot()).isEqualTo(relocated);
        }
    }

    private Tokens createWorkflow() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_USER");
        createTestUser(2L, "worker", "123456", "worker@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        jdbcTemplate.update("UPDATE organization_member SET submit_count = 4 WHERE id = 2");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '类型', 1, 1)");
        jdbcTemplate.update("INSERT INTO project "
                + "(id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) "
                + "VALUES (1, 1, 1, '企划', 'owner-review', '{}', 0, '描述', 1, 1)");
        jdbcTemplate.update("INSERT INTO item (id, project_id, title, task_template_id, creator_id) "
                + "VALUES (1, 1, '项目', 0, 1)");
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) "
                + "VALUES (1, 1, '原子任务', '[]', 1, 1)");
        jdbcTemplate.update("INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) "
                + "VALUES (1, 1, 1, 1, '任务流')");
        insertSlot(1, 1, 1, 2, 2L);
        jdbcTemplate.update("INSERT INTO project_workshop (id, project_id, user_id) VALUES (1, 1, 1), (2, 1, 2)");
        return new Tokens(loginAs("owner", "123456"), loginAs("worker", "123456"));
    }

    private Tokens createReadyWorkflow(String action) {
        Tokens tokens = createWorkflow();
        if (action.equals("reject")) {
            assertCode(submit(tokens.worker(), 1), ResultCode.SUCCESS);
        }
        return tokens;
    }

    private void createOtherProject() {
        jdbcTemplate.update("INSERT INTO project "
                + "(id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) "
                + "VALUES (2, 1, 1, '另一企划', 'other-review', '{}', 0, '描述', 1, 1)");
        jdbcTemplate.update("INSERT INTO item (id, project_id, title, task_template_id, creator_id) VALUES (2, 2, '另一项目', 0, 1)");
        jdbcTemplate.update("INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) VALUES (2, 2, 2, 1, '另一任务流')");
        jdbcTemplate.update("INSERT INTO item_task_node "
                + "(id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                + "VALUES (2, 2, 2, 2, 1, '另一节点', '[]', 9, 1)");
    }

    private void insertSlot(long id, int sort, int parallelSort, int status, Long assigneeId) {
        jdbcTemplate.update("INSERT INTO item_task_node "
                + "(id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                + "VALUES (?, 1, 1, 1, 1, ?, '[]', ?, ?)", id, "节点" + id, sort, parallelSort);
        jdbcTemplate.update("INSERT INTO task_instance "
                + "(id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, claimed_at) "
                + "VALUES (?, 1, 1, ?, 1, ?, ?, ?)", id, id, assigneeId, status,
                assigneeId == null ? null : "2026-10-01 10:00:00");
    }

    private void insertHistory(long id, int version, int status, String comment) {
        jdbcTemplate.update("INSERT INTO task_submission "
                + "(id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, "
                + "status, reviewer_id, reviewed_at, review_comment) "
                + "VALUES (?, 1, 1, 1, 1, 2, ?, '{}', ?, 1, '2026-10-01 12:00:00', ?)",
                id, version, status, comment);
    }

    private Response operate(String token, String action, String expectedRevision, String reason) {
        return authRequest(token).body(body(action, expectedRevision, reason)).post(path(action));
    }

    private static Map<String, String> body(String action, String expectedRevision, String reason) {
        return Map.of("expectedRevision", expectedRevision, action.equals("reject") ? "reviewComment" : "reason", reason);
    }

    private static String path(String action) {
        return "/app/task/instance/1/" + action;
    }

    private Response submit(String token, long instanceId) {
        return authRequest(token).body(Map.of("values", Map.of()))
                .post("/app/task/instance/" + instanceId + "/submit");
    }

    private void assertCode(Response response, ResultCode code) {
        response.then().statusCode(200).body("code", equalTo(code.getCode()));
    }

    private void assertRevision(String token, long instanceId, String expected) {
        Response response = authRequest(token).get("/app/task/instance/item/1");
        assertCode(response, ResultCode.SUCCESS);
        Object actual = response.jsonPath().get("data.find { it.id == '" + instanceId + "' }.revision");
        assertThat(actual).isInstanceOf(String.class).isEqualTo(expected);
    }

    private void assertInstance(long id, int status, Long assigneeId, long revision) {
        Map<String, Object> row = instance(id);
        assertThat(((Number) row.get("status")).intValue()).isEqualTo(status);
        assertThat(row.get("assignee_id")).isEqualTo(assigneeId);
        assertThat(((Number) row.get("revision")).longValue()).isEqualTo(revision);
    }

    private void assertCount(int expected) {
        assertThat(jdbcTemplate.queryForObject("SELECT submit_count FROM organization_member WHERE id = 2", Integer.class))
                .isEqualTo(expected);
    }

    private void assertAudit(String action, String reason, String expectedRevision, String revision, int operationType) {
        List<Map<String, Object>> matching = ownerAudits().stream()
                .filter(row -> action.equals(JSON.parseObject(row.get("request_body").toString()).getString("action")))
                .toList();
        assertThat(matching).hasSize(1);
        Map<String, Object> audit = matching.getFirst();
        assertThat(((Number) audit.get("user_id")).longValue()).isEqualTo(1L);
        assertThat(((Number) audit.get("project_id")).longValue()).isEqualTo(1L);
        assertThat(((Number) audit.get("item_id")).longValue()).isEqualTo(1L);
        assertThat(((Number) audit.get("operation_type")).intValue()).isEqualTo(operationType);
        JSONObject payload = JSON.parseObject(audit.get("request_body").toString());
        assertThat(payload.getString("action")).isEqualTo(action);
        assertThat(payload.getString("reason")).isEqualTo(reason);
        assertThat(payload.getLong("assigneeId")).isEqualTo(2L);
        assertThat(payload.getString("expectedRevision")).isEqualTo(expectedRevision);
        assertThat(payload.getString("revision")).isEqualTo(revision);
    }

    private void pauseFirstLock(CountDownLatch beforeLock, CountDownLatch resume) {
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            if (first.compareAndSet(true, false)) {
                beforeLock.countDown();
                await(resume);
            }
            return invocation.callRealMethod();
        }).when(lockService).tryAcquireOrThrow(eq("task:item:1"), anyLong(), eq(TimeUnit.SECONDS));
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
    }

    private String revision() {
        return jdbcTemplate.queryForObject("SELECT revision FROM task_instance WHERE id = 1", String.class);
    }

    private Map<String, Object> instance(long id) {
        return jdbcTemplate.queryForMap("SELECT * FROM task_instance WHERE id = ?", id);
    }

    private List<Map<String, Object>> instances() {
        return jdbcTemplate.queryForList("SELECT * FROM task_instance ORDER BY id");
    }

    private List<Map<String, Object>> submissions() {
        return jdbcTemplate.queryForList("SELECT * FROM task_submission ORDER BY version_no, id");
    }

    private List<Map<String, Object>> members() {
        return jdbcTemplate.queryForList("SELECT * FROM organization_member ORDER BY id");
    }

    private List<Map<String, Object>> workshops() {
        return jdbcTemplate.queryForList("SELECT * FROM project_workshop ORDER BY id");
    }

    private List<Map<String, Object>> ownerAudits() {
        return jdbcTemplate.queryForList("SELECT * FROM user_operation_log "
                + "WHERE JSON_UNQUOTE(JSON_EXTRACT(request_body, '$.action')) IN ('OWNER_CANCEL', 'OWNER_REJECT') ORDER BY id");
    }

    private Snapshot snapshot() {
        return new Snapshot(instances(), submissions(), members(), workshops(), ownerAudits());
    }

    private record Tokens(String owner, String worker) { }

    private record Snapshot(List<Map<String, Object>> instances, List<Map<String, Object>> submissions,
                            List<Map<String, Object>> members, List<Map<String, Object>> workshops,
                            List<Map<String, Object>> ownerAudits) { }
}
