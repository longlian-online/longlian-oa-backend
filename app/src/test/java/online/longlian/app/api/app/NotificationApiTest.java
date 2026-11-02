package online.longlian.app.api.app;

import io.restassured.response.Response;
import com.alibaba.fastjson2.JSONObject;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.util.MailUtil;
import online.longlian.app.service.outbox.OutboxDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.awaitility.Awaitility;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NotificationApiTest extends BaseApiTest {
    @Autowired StorageProperties storageProperties;
    @Autowired OutboxDispatcher outboxDispatcher;
    @MockBean MailUtil mailUtil;
    String owner;
    String worker;
    String next;
    String other;
    static final String INBOX = "/app/notifications";
    static final String SAME_TIME = "2026-10-22 12:00:00";

    @BeforeEach
    void createFixture() {
        createUserWithOrganization(1,"notifyowner","123456","notifyowner@example.com",1,1,"ORG_USER");
        createTestUser(2,"notifyworker","123456","notifyworker@example.com");
        createTestUser(3,"notifynext","123456","notifynext@example.com");
        createTestUser(4,"notifyother","123456","notifyother@example.com");
        createOrganizationMember(2,1,2,"ORG_USER");
        createOrganizationMember(3,1,3,"ORG_USER");
        createOrganizationMember(4,1,4,"ORG_USER");
        createOrganization(2,"另一组织");
        createOrganizationMember(5,2,1,"ORG_USER");
        createOrganizationMember(6,2,4,"ORG_USER");
        jdbcTemplate.update("INSERT INTO project_type(id,org_id,name,creator_id) VALUES(1,1,'类型',1)");
        jdbcTemplate.update("INSERT INTO project(id,org_id,type_id,title,creator_id) VALUES(10,1,1,'企划原名',1)");
        jdbcTemplate.update("INSERT INTO item(id,project_id,title,task_template_id,creator_id) VALUES(100,10,'项目原名',0,2),(101,10,'未完成的其他项目',0,2)");
        jdbcTemplate.update("INSERT INTO item_task_flow(id,item_id,project_id,task_template_id,name) VALUES(1000,100,10,0,'流程')");
        slot(101,1101,1,1,2L);
        slot(102,1102,1,2,2L);
        slot(201,1201,2,1,3L);
        slot(202,1202,2,2,4L);
        slot(203,1203,2,3,null);
        owner=loginAs("notifyowner","123456");
        worker=loginAs("notifyworker","123456");
        next=loginAs("notifynext","123456");
        other=loginAs("notifyother","123456");
    }

    @Test
    void shouldNotifyOnlyAfterAllParallelPredecessorsAndKeepEachTarget() {
        success(submit(worker,101));
        assertThat(notifications()).isEmpty();
        success(submit(worker,102));
        var rows=notifications();
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> row.get("recipient_user_id")).containsExactly(3L,4L);
        assertThat(rows).extracting(row -> row.get("source_id")).containsExactly(201L,202L);
        assertThat(rows).extracting(row -> row.get("trigger_id")).containsOnly(submissionId(102));
        list(next).then().body("data.list.sourceId",equalTo(List.of("201")))
                .body("data.list.source.itemId",equalTo(List.of("100")));
        list(other).then().body("data.list.sourceId",equalTo(List.of("202")))
                .body("data.list.source.taskSlot.slotIndex",equalTo(List.of(2)));
        success(authRequest(next,1).post("/app/task/instance/203/claim"));
        assertThat(notifications()).hasSize(2);
    }

    @Test
    void shouldOmitBothSlotsWhenSubmitterClaimedThem() {
        jdbcTemplate.update("UPDATE task_instance SET assignee_id=2 WHERE id IN(201,202)");
        success(submit(worker,101));success(submit(worker,102));
        assertThat(notifications()).isEmpty();
        list(worker).then().body("data.list",empty());
    }

    @Test
    void shouldNotifyOnlyTheOtherAssigneeWhenSubmitterClaimedOneSlot() {
        jdbcTemplate.update("UPDATE task_instance SET assignee_id=2 WHERE id=201");
        success(submit(worker,101));success(submit(worker,102));
        var rows=notifications();
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("recipient_user_id")).isEqualTo(4L);
            assertThat(row.get("source_id")).isEqualTo(202L);
        });
        list(worker).then().body("data.list",empty());
        list(other).then().body("data.list.sourceId",equalTo(List.of("202")));
    }

    @Test
    void shouldNotifyPlanningCreatorNotItemCreatorOnLastItemTaskOnly() {
        success(submit(worker,101));success(submit(worker,102));
        success(submit(next,201));success(submit(other,202));
        assertThat(count("SELECT COUNT(*) FROM user_notification WHERE kind=2")).isZero();
        success(authRequest(next,1).post("/app/task/instance/203/claim"));
        success(submit(next,203));
        var completion=jdbcTemplate.queryForMap("SELECT * FROM user_notification WHERE kind=2");
        assertThat(completion.get("recipient_user_id")).isEqualTo(1L);
        assertThat(completion.get("source_id")).isEqualTo(100L);
        assertThat(completion.get("trigger_id")).isEqualTo(submissionId(203));
        assertThat(count("SELECT COUNT(*) FROM user_notification WHERE kind=2 AND recipient_user_id=2")).isZero();
        list(owner).then().body("data.list.source.itemId",equalTo(List.of("100")))
                .body("data.list.source.taskSlot",equalTo(Arrays.asList((Object)null)));
    }

    @Test
    void shouldOmitItemCompletionWhenCreatorSubmitsLastTask() {
        jdbcTemplate.update("UPDATE project SET creator_id=3 WHERE id=10");
        success(submit(worker,101));success(submit(worker,102));
        success(submit(next,201));success(submit(other,202));
        success(authRequest(next,1).post("/app/task/instance/203/claim"));
        success(submit(next,203));
        assertThat(count("SELECT COUNT(*) FROM user_notification WHERE kind=2")).isZero();
    }

    @Test
    void shouldAllowOnlyOneConcurrentSubmissionEventForSameTask() throws Exception {
        var responses=underFlowLock(List.of(() -> submit(worker,101),() -> submit(worker,101)));
        assertThat(responses).extracting(r -> r.jsonPath().getInt("code"))
                .containsExactlyInAnyOrder(0,ResultCode.OPERATION_FAIL.getCode());
        assertThat(count("SELECT COUNT(*) FROM task_submission WHERE task_instance_id=101")).isEqualTo(1);
        assertThat(notifications()).isEmpty();
        assertThat(submit(worker,101).jsonPath().getInt("code")).isNotZero();
    }

    @Test
    void shouldSerializeParallelSubmissionsAndEmitOneUnlockEvent() throws Exception {
        var responses=underFlowLock(List.of(() -> submit(worker,101),() -> submit(worker,102)));
        responses.forEach(this::success);
        assertThat(notifications()).hasSize(2);
        assertThat(count("SELECT COUNT(DISTINCT trigger_id) FROM user_notification")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM task_submission")).isEqualTo(2);
    }

    @Test
    void shouldRollbackTaskAndSubmissionWhenNotificationInsertFails() {
        success(submit(worker,101));
        jdbcTemplate.execute("CREATE TRIGGER fail_notification BEFORE INSERT ON user_notification FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='notification write failure'");
        try {
            assertThat(submit(worker,102).jsonPath().getInt("code")).isNotZero();
            assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id=102",Integer.class)).isEqualTo(2);
            assertThat(count("SELECT COUNT(*) FROM task_submission WHERE task_instance_id=102")).isZero();
            assertThat(notifications()).isEmpty();
        } finally {jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_notification");}
        success(submit(worker,102));
        assertThat(notifications()).hasSize(2);
    }

    @Test
    void shouldEnqueueInboxEmailOnlyForNotificationsCreatedBySubmit() {
        success(submit(worker,101));
        assertThat(notifications()).isEmpty();
        assertThat(outbox()).isEmpty();
        success(submit(worker,102));
        var notes=notifications();
        var rows=outbox();
        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row -> {
            assertThat(((Number) row.get("type")).intValue()).isEqualTo(1);
            assertThat(((Number) row.get("status")).intValue()).isEqualTo(1);
        });
        assertThat(rows).extracting(row -> String.valueOf(row.get("dedupe_key")))
                .containsExactlyInAnyOrderElementsOf(notes.stream().map(note -> String.valueOf(note.get("id"))).toList());
        assertThat(rows).extracting(row -> payloadOf(row).getString("kind")).containsOnly("TASK_READY");
        assertThat(rows).extracting(row -> payloadOf(row).getLong("recipientUserId")).containsExactlyInAnyOrder(3L,4L);
        assertThat(rows).extracting(row -> payloadOf(row).getLong("sourceId")).containsExactlyInAnyOrder(201L,202L);
    }

    @Test
    void shouldEnqueueItemCompletionEmailOnlyForPlanningOwner() {
        success(submit(worker,101));success(submit(worker,102));
        success(submit(next,201));success(submit(other,202));
        success(authRequest(next,1).post("/app/task/instance/203/claim"));
        success(submit(next,203));
        var completion=jdbcTemplate.queryForMap("SELECT * FROM user_notification WHERE kind=2");
        var completed=outbox().stream().filter(row -> "ITEM_COMPLETED".equals(payloadOf(row).getString("kind"))).toList();
        assertThat(completed).singleElement().satisfies(row -> {
            assertThat(String.valueOf(row.get("dedupe_key"))).isEqualTo(String.valueOf(completion.get("id")));
            assertThat(payloadOf(row).getLong("recipientUserId")).isEqualTo(1L);
            assertThat(payloadOf(row).getLong("sourceId")).isEqualTo(100L);
        });
        assertThat(completed).extracting(row -> payloadOf(row).getLong("recipientUserId")).doesNotContain(2L);
    }

    @Test
    void shouldKeepInboxButSkipEmailWhenRecipientDisabledThatKind() {
        success(userRequest(next).body(Map.of("config", Map.of("notificationKinds", Map.of("TASK_READY", false))))
                .patch("/app/user/config"));

        success(submit(worker, 101));
        success(submit(worker, 102));

        assertThat(notifications()).extracting(row -> row.get("recipient_user_id")).containsExactly(3L, 4L);
        list(next).then().body("data.list.sourceId", equalTo(List.of("201")));

        outboxDispatcher.dispatch(testNow().plusSeconds(1));

        assertThat(emailStatuses(3L)).containsExactly(5);
        assertThat(emailStatuses(4L)).containsExactly(3);
        verify(mailUtil, never()).send(argThat(mail -> mail.receiver().equals("notifynext@example.com")));
        verify(mailUtil).send(argThat(mail -> mail.receiver().equals("notifyother@example.com")));
    }

    @Test
    void shouldSkipQueuedMailWhenKindDisabledAfterItWasCreated() {
        success(submit(worker, 101));
        success(submit(worker, 102));
        success(userRequest(next).body(Map.of("config", Map.of("notificationKinds", Map.of("TASK_READY", false))))
                .patch("/app/user/config"));

        outboxDispatcher.dispatch(testNow().plusSeconds(1));

        assertThat(emailStatuses(3L)).containsExactly(5);
        assertThat(emailStatuses(4L)).containsExactly(3);
        verify(mailUtil, never()).send(argThat(mail -> mail.receiver().equals("notifynext@example.com")));
        verify(mailUtil).send(argThat(mail -> mail.receiver().equals("notifyother@example.com")));
        list(next).then().body("data.list.sourceId", equalTo(List.of("201")));
    }

    @Test
    void shouldSkipQueuedTaskMailAfterDisablingAndSendNewMailAfterEnabling() {
        success(submit(worker, 101));
        success(submit(worker, 102));
        setEmailNotifications(next, false);

        outboxDispatcher.dispatch(testNow().plusSeconds(1));

        assertThat(emailStatuses(3L)).containsExactly(5);
        assertThat(emailStatuses(4L)).containsExactly(3);
        verify(mailUtil, never()).send(argThat(mail -> mail.receiver().equals("notifynext@example.com")));
        verify(mailUtil).send(argThat(mail -> mail.receiver().equals("notifyother@example.com")));
        list(next).then().body("data.list.sourceId", equalTo(List.of("201")));

        setEmailNotifications(next, true);
        success(authRequest(worker, 1).post("/app/task/instance/101/reset"));
        success(submit(worker, 101));
        outboxDispatcher.dispatch(testNow().plusSeconds(2));

        assertThat(emailStatuses(3L)).containsExactly(5, 3);
        verify(mailUtil).send(argThat(mail -> mail.receiver().equals("notifynext@example.com")));
    }

    @Test
    void shouldSkipProjectCompletionMailWithoutSuppressingInboxOrOtherRecipients() {
        success(submit(worker, 101));
        success(submit(worker, 102));
        success(submit(next, 201));
        success(submit(other, 202));
        success(authRequest(next, 1).post("/app/task/instance/203/claim"));
        success(submit(next, 203));
        setEmailNotifications(owner, false);

        outboxDispatcher.dispatch(testNow().plusSeconds(1));

        assertThat(emailStatuses(1L)).containsExactly(5);
        assertThat(emailStatuses(3L)).containsExactly(3);
        assertThat(emailStatuses(4L)).containsExactly(3);
        verify(mailUtil, never()).send(argThat(mail -> mail.receiver().equals("notifyowner@example.com")));
        list(owner).then().body("data.list.sourceId", equalTo(List.of("100")));
    }

    @Test
    void shouldOmitItemCompletionEmailWhenOwnerSubmitsLastTask() {
        jdbcTemplate.update("UPDATE project SET creator_id=3 WHERE id=10");
        success(submit(worker,101));success(submit(worker,102));
        success(submit(next,201));success(submit(other,202));
        success(authRequest(next,1).post("/app/task/instance/203/claim"));
        success(submit(next,203));
        assertThat(count("SELECT COUNT(*) FROM user_notification WHERE kind=2")).isZero();
        assertThat(outbox().stream().map(row -> payloadOf(row).getString("kind"))).doesNotContain("ITEM_COMPLETED");
    }

    @Test
    void shouldRollbackSubmissionWhenOutboxInsertFails() {
        success(submit(worker,101));
        jdbcTemplate.execute("CREATE TRIGGER fail_outbox_message BEFORE INSERT ON outbox_message FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='outbox write failure'");
        try {
            assertThat(submit(worker,102).jsonPath().getInt("code")).isNotZero();
            assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id=102",Integer.class)).isEqualTo(2);
            assertThat(count("SELECT COUNT(*) FROM task_submission WHERE task_instance_id=102")).isZero();
            assertThat(notifications()).isEmpty();
            assertThat(outbox()).isEmpty();
        } finally {jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_outbox_message");}
    }

    @Test
    void shouldReunlockDistantStageAfterResetAndPreserveBothEvents() {
        jdbcTemplate.update("UPDATE task_instance SET status=3 WHERE id IN(102,201,202,203)");
        slot(301,1301,3,1,3L);jdbcTemplate.update("UPDATE task_instance SET status=3 WHERE id=301");
        slot(401,1401,4,1,3L);
        success(submit(worker,101));
        long firstTrigger=submissionId(101);
        assertThat(notifications()).extracting(row -> row.get("source_id")).containsExactly(401L);
        success(authRequest(worker,1).post("/app/task/instance/101/reset"));
        assertThat(notifications()).hasSize(1);
        success(submit(worker,101));
        var rows=notifications();
        assertThat(rows).extracting(row -> row.get("source_id")).containsExactly(401L,401L);
        assertThat(rows).extracting(row -> row.get("trigger_id")).containsExactlyInAnyOrder(firstTrigger,submissionId(101));
        list(next).then().body("data.list.size()",equalTo(2)).body("data.list.sourceId",everyItem(equalTo("401")));
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO user_notification(id,org_id,recipient_user_id,kind,source_id,trigger_id) VALUES(9999,1,3,1,401,?)",firstTrigger))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void shouldNotNotifyForErrorsResetOrRejectItself() {
        assertThat(submit(next,201).jsonPath().getInt("code")).isNotZero();
        assertThat(submit(owner,101).jsonPath().getInt("code")).isNotZero();
        success(submit(worker,101));
        success(authRequest(worker,1).post("/app/task/instance/101/reset"));
        success(submit(worker,101));
        String revision=jdbcTemplate.queryForObject("SELECT revision FROM task_instance WHERE id=101",String.class);
        success(authRequest(owner,1).body(Map.of("expectedRevision",revision,"reviewComment","重新处理"))
                .post("/app/task/instance/101/reject"));
        assertThat(notifications()).isEmpty();
    }

    @Test
    void shouldPageSameSecondBigintIdsStablyAcrossNewInserts() {
        long base=9_007_199_254_741_000L;
        for(int i=1;i<=25;i++) notification(base+i,1L,1,2,100,5000+i,SAME_TIME);
        var first=list(owner);success(first);
        assertThat(first.jsonPath().getList("data.list.id",String.class))
                .containsExactlyElementsOf(LongStream.rangeClosed(6,25).mapToObj(i -> Long.toString(base+31-i)).toList());
        first.then().body("data.hasNext",equalTo(true)).body("data.unreadCount",equalTo(25));
        notification(base+26,1L,1,2,100,5026,SAME_TIME);
        Response second=authRequest(owner,1).queryParam("beforeCreatedAt",SAME_TIME).queryParam("beforeId",Long.toString(base+6)).queryParam("beforeUnread",true).get(INBOX);
        success(second);
        assertThat(second.jsonPath().getList("data.list.id",String.class)).containsExactly(
                Long.toString(base+5),Long.toString(base+4),Long.toString(base+3),Long.toString(base+2),Long.toString(base+1));
        second.then().body("data.hasNext",equalTo(false)).body("data.unreadCount",equalTo(26));
        authRequest(owner,1).queryParam("beforeCreatedAt",SAME_TIME).queryParam("beforeId",Long.toString(base+1)).queryParam("beforeUnread",true).get(INBOX)
                .then().body("code",equalTo(0)).body("data.list",empty()).body("data.unreadCount",equalTo(26));
    }

    @Test
    void shouldKeepUnreadAheadOfNewerReadNotifications() {
        notification(11,1L,1,2,100,11,"2026-10-22 09:00:00");
        notification(12,1L,1,2,100,12,"2026-10-22 13:00:00");
        notification(13,1L,1,2,100,13,"2026-10-22 10:00:00");
        notification(14,1L,1,2,100,14,"2026-10-22 08:00:00");
        jdbcTemplate.update("UPDATE user_notification SET read_at='2026-10-22 13:30:00' WHERE id=12");
        jdbcTemplate.update("UPDATE user_notification SET read_at='2026-10-22 14:00:00' WHERE id=14");
        var page=list(owner);
        success(page);
        assertThat(page.jsonPath().getList("data.list.id",String.class)).containsExactly("13","11","12","14");
        Response next=authRequest(owner,1).queryParam("beforeCreatedAt","2026-10-22 09:00:00").queryParam("beforeId","11").queryParam("beforeUnread",true).get(INBOX);
        success(next);
        assertThat(next.jsonPath().getList("data.list.id",String.class)).containsExactly("12","14");
        authRequest(owner,1).queryParam("beforeCreatedAt","2026-10-22 13:00:00").queryParam("beforeId","12").queryParam("beforeUnread",false).get(INBOX)
                .then().body("data.list.id",equalTo(List.of("14")));
    }

    @ParameterizedTest
    @ValueSource(strings={"beforeId=1","beforeCreatedAt=2026-10-22%2012:00:00","beforeCreatedAt=bad&beforeId=1",
            "beforeCreatedAt=2026-10-22%2012:00:00&beforeId=0","beforeCreatedAt=2026-10-22%2012:00:00&beforeId=-1",
            "beforeCreatedAt=2026-10-22%2012:00:00&beforeId=9223372036854775808",
            "beforeCreatedAt=2026-10-22%2012:00:00&beforeId=1","beforeUnread=true"})
    void shouldRejectInvalidCursor(String query) {
        authRequest(owner,1).urlEncodingEnabled(false).get(INBOX+"?"+query).then().body("code",equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldKeepReadScopeAndOriginalTimeAndReadAllUnloadedRows() {
        for(int i=1;i<=25;i++)notification(10000+i,1L,1,2,100,20000+i,SAME_TIME);
        notification(20001,1L,3,2,100,30001,SAME_TIME);
        notification(20002,2L,1,2,100,30002,SAME_TIME);
        notification(20003,null,1,2,100,30003,SAME_TIME);
        assertThat(authRequest(next,1).put(INBOX+"/10001/read").jsonPath().getInt("code")).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        assertThat(authRequest(owner,2).put(INBOX+"/10001/read").jsonPath().getInt("code")).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        Response first=authRequest(owner,1).put(INBOX+"/10001/read");success(first);
        String time=first.jsonPath().getString("data.readAt");
        authRequest(owner,1).put(INBOX+"/10001/read").then().body("code",equalTo(0)).body("data.readAt",equalTo(time));
        list(owner).then().body("data.list.size()",equalTo(20)).body("data.unreadCount",equalTo(24));
        success(authRequest(owner,1).put(INBOX+"/read-all"));
        assertThat(count("SELECT COUNT(*) FROM user_notification WHERE org_id=1 AND recipient_user_id=1 AND read_at IS NULL")).isZero();
        assertThat(count("SELECT COUNT(*) FROM user_notification WHERE id IN(20001,20002,20003) AND read_at IS NULL")).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT created_at FROM user_notification WHERE id=10001",LocalDateTime.class))
                .isEqualTo(LocalDateTime.of(2026,10,22,12,0));
    }

    @Test
    void shouldReadCurrentNamesAndInvalidateOnlyUnavailableSourceAtSamePosition() {
        notification(1,1L,1,1,201,1001,SAME_TIME);notification(2,1L,1,2,100,1002,SAME_TIME);
        list(owner).then().body("data.list.id",equalTo(List.of("2","1")));
        jdbcTemplate.update("UPDATE project SET title='新企划',status=3 WHERE id=10");
        jdbcTemplate.update("UPDATE item SET title='新项目' WHERE id=100");
        jdbcTemplate.update("UPDATE task_instance SET assignee_id=4,status=1 WHERE id=201");
        var current=list(owner);success(current);
        assertThat(current.jsonPath().getList("data.list.source.projectTitle",String.class)).containsOnly("新企划");
        assertThat(current.jsonPath().getList("data.list.source.itemTitle",String.class)).containsOnly("新项目");
        jdbcTemplate.update("UPDATE item_task_node SET deleted_at=NOW() WHERE id=1201");
        var missingNode=list(owner);success(missingNode);
        assertThat(missingNode.jsonPath().getList("data.list.id",String.class)).containsExactly("2","1");
        assertThat(missingNode.jsonPath().getString("data.list[0].source.itemId")).isEqualTo("100");
        assertUnavailable(missingNode,1);
        success(authRequest(owner,1).put(INBOX+"/1/read"));
        jdbcTemplate.update("UPDATE project SET resource_status=0 WHERE id=10");
        var disabled=list(owner);assertUnavailable(disabled,0);assertUnavailable(disabled,1);
    }

    @ParameterizedTest
    @ValueSource(strings={"UPDATE task_instance SET deleted_at=NOW() WHERE id=201",
            "UPDATE item SET deleted_at=NOW() WHERE id=100","UPDATE project SET deleted_at=NOW() WHERE id=10",
            "UPDATE project SET org_id=2,title='秘密名称' WHERE id=10"})
    void shouldKeepInaccessibleNotificationWithoutLeakingContentOrImages(String change) {
        notification(1,1L,1,1,201,1001,SAME_TIME);
        jdbcTemplate.update(change);
        var response=list(owner);success(response);assertUnavailable(response,0);
        assertThat(response.asString()).doesNotContain("秘密名称");
        success(authRequest(owner,1).put(INBOX+"/1/read"));
    }

    @Test
    void shouldResolveMixedKindsWithIdenticalSourceNumberAndIndependentHistory() {
        slot(100,1204,2,4,3L);
        notification(1,1L,1,1,100,8001,"2026-10-21 10:00:00");
        notification(2,1L,1,2,100,8001,SAME_TIME);
        notification(3,1L,1,1,100,8002,SAME_TIME);
        success(authRequest(owner,1).put(INBOX+"/1/read"));
        var response=list(owner);success(response);
        response.then().body("data.list.id",equalTo(List.of("3","2","1")))
                .body("data.list[0].source.taskSlot.slotIndex",equalTo(4))
                .body("data.list[1].source.taskSlot",nullValue())
                .body("data.list[2].createdAt",equalTo("2026-10-21 10:00:00"));
        assertThat(response.jsonPath().getString("data.list[2].readAt")).isNotNull();
        assertThat(response.jsonPath().getString("data.list[0].readAt")).isNull();
    }

    @Test
    void shouldPreferThumbnailThenCoverAndNeverReturnCrossOrganizationImage() {
        createResource(700,1,1);createResource(701,1,1);createResource(702,2,1);
        jdbcTemplate.update("UPDATE project SET thumbnail_file_id=700,cover_file_id=701 WHERE id=10");
        notification(1,1L,1,2,100,1001,SAME_TIME);
        String thumbnail=list(owner).jsonPath().getString("data.list[0].thumbnailUrl");
        assertThat(thumbnail).contains("700.png","eo-img.resize=w/144/h/144");
        jdbcTemplate.update("UPDATE resource SET process_status=0 WHERE id=700");
        assertThat(list(owner).jsonPath().getString("data.list[0].thumbnailUrl")).contains("701.png");
        jdbcTemplate.update("UPDATE project SET thumbnail_file_id=702,cover_file_id=702 WHERE id=10");
        assertThat(list(owner).jsonPath().getString("data.list[0].thumbnailUrl")).isNull();
        jdbcTemplate.update("UPDATE project SET thumbnail_file_id=701 WHERE id=10");
        var cdn=storageProperties.getCdn();
        boolean wasEnabled=cdn.isEnabled();String prefix=cdn.getUrlPrefix();String key=cdn.getAuthKey();
        try {
            cdn.setEnabled(true);cdn.setUrlPrefix("http://localhost");cdn.setAuthKey("notification-test-key");
            assertThat(list(owner).jsonPath().getString("data.list[0].thumbnailUrl"))
                    .contains("eo-img.resize=w/144/h/144","/common/file/local");
        } finally {cdn.setEnabled(wasEnabled);cdn.setUrlPrefix(prefix);cdn.setAuthKey(key);}
    }

    @Test
    void shouldReportUnreadOnlyForCurrentUserAndOrganizationEvenWithMissingSource() {
        status(owner,1,false);
        notification(1,1L,3,2,100,1001,SAME_TIME);
        notification(2,2L,1,2,100,1002,SAME_TIME);
        notification(3,null,1,2,100,1003,SAME_TIME);
        notification(4,1L,1,2,100,1004,SAME_TIME);
        jdbcTemplate.update("UPDATE user_notification SET deleted_at=NOW() WHERE id=4");
        status(owner,1,false);status(next,1,true);status(owner,2,true);
        notification(5,1L,1,2,999999,1005,SAME_TIME);
        status(owner,1,true);
        var data=authRequest(owner,1).get(INBOX+"/status").jsonPath().getMap("data");
        assertThat(data).containsOnlyKeys("hasUnread");
        success(authRequest(owner,1).put(INBOX+"/read-all"));status(owner,1,false);
        assertThat(jdbcTemplate.queryForObject("SELECT read_at FROM user_notification WHERE id=3",LocalDateTime.class)).isNull();
        list(owner).then().body("data.list.id",equalTo(List.of("5"))).body("data.unreadCount",equalTo(0));
    }

    @Test
    void shouldReturnEmptyInboxAndRejectUnauthorizedOrInactiveMembership() {
        list(owner).then().body("code",equalTo(0)).body("data.list",empty()).body("data.hasNext",equalTo(false)).body("data.unreadCount",equalTo(0));
        for(String path:List.of(INBOX,INBOX+"/status")) {
            assertThat(request().get(path).jsonPath().getInt("code")).isNotZero();
            assertThat(userRequest(owner).get(path).jsonPath().getInt("code")).isNotZero();
            createOrganization(3,"未加入组织");
            assertThat(authRequest(owner,3).get(path).jsonPath().getInt("code")).isNotZero();
            jdbcTemplate.update("DELETE FROM organization WHERE id=3");
        }
        jdbcTemplate.update("UPDATE organization_member SET status=0 WHERE id=1");
        assertThat(list(owner).jsonPath().getInt("code")).isNotZero();
        assertThat(authRequest(owner,1).get(INBOX+"/status").jsonPath().getInt("code")).isNotZero();
    }

    private void setEmailNotifications(String token, boolean enabled) {
        success(userRequest(token).body(Map.of("config", Map.of("emailNotificationsEnabled", enabled)))
                .patch("/app/user/config"));
    }

    private List<Integer> emailStatuses(long recipient) {
        return jdbcTemplate.queryForList("SELECT status FROM outbox_message "
                + "WHERE JSON_EXTRACT(payload, '$.recipientUserId') = ? ORDER BY id", Integer.class, recipient);
    }

    private void slot(long instance,long node,int sort,int parallel,Long assignee) {
        jdbcTemplate.update("INSERT INTO item_task_node(id,item_task_flow_id,item_id,project_id,base_task_id,name,meta_schema,sort,parallel_sort) VALUES(?,1000,100,10,1,'翻译','[]',?,?)",node,sort,parallel);
        jdbcTemplate.update("INSERT INTO task_instance(id,project_id,item_id,item_task_node_id,task_flow_id,assignee_id,status) VALUES(?,10,100,?,1000,?,?)",instance,node,assignee,assignee==null?1:2);
    }
    private void notification(long id,Long org,long recipient,int kind,long source,long trigger,String createdAt) {
        jdbcTemplate.update("INSERT INTO user_notification(id,org_id,recipient_user_id,kind,source_id,trigger_id,created_at) VALUES(?,?,?,?,?,?,?)",id,org,recipient,kind,source,trigger,createdAt);
    }
    private Response submit(String token,long id) {return authRequest(token,1).body(Map.of("values",Map.of())).post("/app/task/instance/"+id+"/submit");}
    private Response list(String token) {return authRequest(token,1).get(INBOX);}
    private void success(Response response) {response.then().statusCode(200).body("code",equalTo(0));}
    private long count(String sql) {return jdbcTemplate.queryForObject(sql,Long.class);}
    private long submissionId(long instance) {return jdbcTemplate.queryForObject("SELECT id FROM task_submission WHERE task_instance_id=? ORDER BY version_no DESC LIMIT 1",Long.class,instance);}
    private List<Map<String,Object>> notifications() {return jdbcTemplate.queryForList("SELECT * FROM user_notification ORDER BY source_id,trigger_id");}
    private List<Map<String,Object>> outbox() {return jdbcTemplate.queryForList("SELECT * FROM outbox_message ORDER BY id");}
    private JSONObject payloadOf(Map<String,Object> row) {
        Object payload=row.get("payload");
        String json=payload instanceof byte[] bytes ? new String(bytes, java.nio.charset.StandardCharsets.UTF_8) : String.valueOf(payload);
        return JSONObject.parseObject(json);
    }
    private void status(String token,long org,boolean expected) {authRequest(token,org).get(INBOX+"/status").then().body("code",equalTo(0)).body("data.hasUnread",equalTo(expected));}
    private void assertUnavailable(Response response,int index) {
        var notification=response.jsonPath().getMap("data.list["+index+"]");
        assertThat(notification).containsEntry("source",null).containsEntry("thumbnailUrl",null);
        assertThat(notification).doesNotContainKeys("content","message","label","targetPath");
    }
    private List<Response> underFlowLock(List<Supplier<Response>> requests) throws Exception {
        try(Connection blocker=jdbcTemplate.getDataSource().getConnection();var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            blocker.setAutoCommit(false);
            try(var statement=blocker.createStatement();var rows=statement.executeQuery("SELECT id FROM item_task_flow WHERE item_id=100 FOR UPDATE")) {assertThat(rows.next()).isTrue();}
            List<Future<Response>> futures=requests.stream().map(request -> executor.submit(() -> request.get())).toList();
            try {
                Awaitility.await().atMost(15,TimeUnit.SECONDS).untilAsserted(() -> assertThat(count(
                        "SELECT COUNT(DISTINCT w.requesting_trx_id) FROM information_schema.INNODB_LOCK_WAITS w JOIN information_schema.INNODB_LOCKS l ON l.lock_id=w.requested_lock_id WHERE l.lock_table=CONCAT('`',DATABASE(),'`.`item_task_flow`')"))
                        .isGreaterThanOrEqualTo(requests.size()));
            } finally {blocker.commit();}
            List<Response> responses=new ArrayList<>();for(var future:futures)responses.add(future.get(20,TimeUnit.SECONDS));
            return responses;
        }
    }
}
