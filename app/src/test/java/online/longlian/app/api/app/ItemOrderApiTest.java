package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemReorderParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAccessParamsBO;
import online.longlian.app.service.app.ItemService;
import online.longlian.app.service.app.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

public class ItemOrderApiTest extends BaseApiTest {
    private static final List<String> INITIAL = List.of("1", "2", "3", "4", "5", "6");
    private static final List<String> REORDERED = List.of("1", "5", "2", "3", "4", "6");

    @Autowired private ItemService itemService;
    @Autowired private ProjectService projectService;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void shouldPersistSharedOrderAcrossPagesWithoutChangingTimestamps() {
        String token = seed();
        var timestamps = jdbcTemplate.queryForList("SELECT id, created_at, updated_at FROM item ORDER BY id");
        page(token, 1, null).then().body("data.list.id", equalTo(INITIAL.subList(0, 4)));
        page(token, 2, null).then().body("data.list.id", equalTo(INITIAL.subList(4, 6)));

        save(token, INITIAL, REORDERED).then().body("code", equalTo(ResultCode.SUCCESS.getCode()));

        page(token, 1, null).then().body("data.list.id", equalTo(REORDERED.subList(0, 4)));
        page(token, 2, null).then().body("data.list.id", equalTo(REORDERED.subList(4, 6)));
        assertThat(jdbcTemplate.queryForList("SELECT id, created_at, updated_at FROM item ORDER BY id"))
                .isEqualTo(timestamps);
        assertThat(databaseOrder()).containsExactly(1L, 5L, 2L, 3L, 4L, 6L);
        save(token, INITIAL, REORDERED).then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(databaseOrder()).containsExactly(1L, 5L, 2L, 3L, 4L, 6L);
    }

    @Test
    void shouldFilterTheGlobalOrderBeforePagination() {
        String token = seed();
        jdbcTemplate.update("INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, status) VALUES (1,1,3,1,1,3)");
        save(token, INITIAL, REORDERED).then().body("code", equalTo(0));

        authRequest(token).queryParam("status", "IN_PROGRESS").queryParam("pageSize", 4)
                .get("/app/projects/1/items").then()
                .body("data.total", equalTo(5)).body("data.list.id", equalTo(List.of("1", "5", "2", "4")));
        authRequest(token).queryParam("status", "COMPLETED").get("/app/projects/1/items").then()
                .body("data.total", equalTo(1)).body("data.list.id", equalTo(List.of("3")));
        authRequest(token).queryParam("status", "").queryParam("pageSize", 100)
                .get("/app/projects/1/items").then().body("data.list.id", equalTo(REORDERED));
        authRequest(token).queryParam("status", "COMPLETED").queryParam("pageSize", 1)
                .get("/app/projects/1/items/order").then().body("data.id", equalTo(REORDERED));
    }

    @Test
    void shouldSearchTitlesBeforeManualOrderPagination() {
        String token = seed();
        jdbcTemplate.update("UPDATE item SET title=CONCAT('MATCH-',title) WHERE id IN (1,3,5)");
        save(token, INITIAL, REORDERED).then().body("code", equalTo(0));

        authRequest(token).queryParam("keyword", " MATCH ").queryParam("status", "")
                .queryParam("pageSize", 2).queryParam("pageNum", 1)
                .get("/app/projects/1/items").then()
                .body("code", equalTo(0)).body("data.total", equalTo(3))
                .body("data.list.id", equalTo(List.of("1", "5")));
        authRequest(token).queryParam("keyword", " MATCH ").queryParam("status", "")
                .queryParam("pageSize", 2).queryParam("pageNum", 2)
                .get("/app/projects/1/items").then()
                .body("code", equalTo(0)).body("data.total", equalTo(3))
                .body("data.list.id", equalTo(List.of("3")));
    }

    @Test
    void shouldExposeDeletionRequestEligibilityOnlyForCurrentSubmissions() {
        String token = seed();
        jdbcTemplate.update("""
                INSERT INTO task_submission
                    (id, project_id, item_id, task_instance_id, item_task_node_id,
                     submitter_id, version_no, status, deleted_at)
                VALUES (102,1,2,102,102,1,1,1,NULL),
                       (103,1,3,103,103,1,1,2,NULL),
                       (104,1,4,104,104,1,1,1,NOW()),
                       (105,1,5,105,105,1,1,3,NULL)
                """);
        authRequest(token).queryParam("status", "").queryParam("pageSize", 100)
                .get("/app/projects/1/items").then()
                .body("code", equalTo(0)).body("data.list.id", equalTo(INITIAL))
                .body("data.list.hasSubmittedTask", equalTo(List.of(false, true, false, false, false, false)));

        save(token, INITIAL, REORDERED).then().body("code", equalTo(0));
        authRequest(token).queryParam("status", "").queryParam("pageSize", 100)
                .get("/app/projects/1/items").then()
                .body("code", equalTo(0)).body("data.list.id", equalTo(REORDERED))
                .body("data.list.hasSubmittedTask", equalTo(List.of(false, false, true, false, false, false)));
    }

    @Test
    void shouldRejectDeletionRequestsForInvalidItemsOrMissingSubmissions() {
        String token = seed();
        jdbcTemplate.update("INSERT INTO project (id,org_id,type_id,title,creator_id) VALUES (2,1,1,'其他企划',1)");
        jdbcTemplate.update("INSERT INTO item (id,project_id,title,sort,task_template_id,creator_id) VALUES (7,2,'其他企划项目',1,1,1)");
        jdbcTemplate.update("INSERT INTO item (id,project_id,title,sort,task_template_id,creator_id,deleted_at) VALUES (8,1,'已删除项目',7,1,1,NOW())");
        for (long id : List.of(99L, 7L, 8L)) {
            authRequest(token).body(Map.of("reason", "申请删除"))
                    .post("/app/projects/1/items/" + id + "/deletion-requests").then()
                    .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        }
        authRequest(token).body(Map.of("reason", "尚无提交"))
                .post("/app/projects/1/items/1/deletion-requests").then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deletion_request", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM item WHERE id=1", LocalDateTime.class)).isNull();
    }

    @Test
    void shouldKeepTimeSortsIndependentWithStableTimestampTies() {
        String token = seed();
        jdbcTemplate.update("UPDATE item SET created_at='2026-01-01', updated_at='2026-02-01' WHERE id=2");
        save(token, INITIAL, REORDERED).then().body("code", equalTo(0));
        page(token, 1, "CREATED_ASC").then().body("data.list.id", equalTo(List.of("1", "2", "3", "4")));
        page(token, 1, "CREATED_DESC").then().body("data.list.id", equalTo(List.of("6", "5", "4", "3")));
        page(token, 1, "UPDATED_ASC").then().body("data.list.id", equalTo(List.of("1", "2", "3", "4")));
        page(token, 1, "UPDATED_DESC").then().body("data.list.id", equalTo(List.of("6", "5", "4", "3")));
        page(token, 1, "MANUAL").then().body("data.list.id", equalTo(REORDERED.subList(0, 4)));
    }

    @Test
    void shouldRejectStaleOrderInsteadOfOverwritingAnotherEditor() {
        String token = seed();
        save(token, INITIAL, REORDERED).then().body("code", equalTo(0));
        save(token, INITIAL, List.of("6", "5", "4", "3", "2", "1")).then()
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(databaseOrder()).containsExactly(1L, 5L, 2L, 3L, 4L, 6L);
    }

    @Test
    void shouldAppendNewItemsAndRejectAnObsoleteMembershipSnapshot() {
        String token = seed();
        authRequest(token).body(Map.of("title", "G", "taskTemplateId", "1"))
                .post("/app/projects/1/items").then().body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject("SELECT sort FROM item WHERE title='G'", Long.class)).isEqualTo(7L);
        save(token, INITIAL, REORDERED).then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(databaseOrder().subList(0, 6)).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    void shouldExcludeReviewedDeletedItemsWithoutRenumberingTheOthers() {
        String token = seed();
        var before = jdbcTemplate.queryForList("SELECT id, sort FROM item WHERE id <> 2 ORDER BY id");
        itemService.deleteProjectItemForReview(1L, 2L, 1L);
        assertThat(jdbcTemplate.queryForList("SELECT id, sort FROM item WHERE id <> 2 ORDER BY id")).isEqualTo(before);
        authRequest(token).get("/app/projects/1/items/order").then()
                .body("data.id", equalTo(List.of("1", "3", "4", "5", "6")));
        save(token, INITIAL, REORDERED).then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(databaseOrder()).containsExactly(1L, 3L, 4L, 5L, 6L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"expectedItemIds\":null,\"itemIds\":[]}",
            "{\"expectedItemIds\":[\"1\"],\"itemIds\":null}",
            "{\"expectedItemIds\":[\"1\",null],\"itemIds\":[\"1\",\"2\"]}",
            "{\"expectedItemIds\":[\"1\"],\"itemIds\":[\"0\"]}",
            "{\"expectedItemIds\":[\"1\",\"2\"],\"itemIds\":[\"1\",\"1\"]}",
            "{\"expectedItemIds\":[\"1\",\"1\"],\"itemIds\":[\"1\",\"2\"]}",
            "{\"expectedItemIds\":[\"1\",\"2\"],\"itemIds\":[\"2\",\"3\"]}"
    })
    void shouldRejectInvalidOrderArraysWithoutWriting(String body) {
        String token = seed();
        authRequest(token).body(body).put("/app/projects/1/items/order").then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(databaseOrder()).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    void shouldRejectMissingForeignAndDeletedIdsAtomically() {
        String token = seed();
        jdbcTemplate.update("INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (7,2,'foreign',1,1,1)");
        save(token, List.of("1", "2"), List.of("2", "1")).then().body("code", equalTo(5));
        save(token, List.of("1", "2", "3", "4", "5", "7"), List.of("7", "5", "4", "3", "2", "1"))
                .then().body("code", equalTo(5));
        assertThat(databaseOrder()).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
        assertThat(jdbcTemplate.queryForObject("SELECT sort FROM item WHERE id=7", Long.class)).isEqualTo(1L);
    }

    @Test
    void shouldAllowEmptyOrderAndRejectUnknownSortMode() {
        String token = seed();
        authRequest(token).queryParam("sortBy", "INVALID").get("/app/projects/1/items").then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        jdbcTemplate.update("UPDATE item SET deleted_at=NOW() WHERE project_id=1");
        authRequest(token).get("/app/projects/1/items/order").then().body("data", equalTo(List.of()));
        save(token, List.of(), List.of()).then().body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM item WHERE deleted_at IS NULL", Long.class)).isZero();
    }

    @Test
    void shouldRestrictOrderManagementToTheParentCreator() {
        String token = seed();
        createTestUser(2L, "admin-member", "123456", "admin-member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_ADMIN");
        jdbcTemplate.update("UPDATE user SET default_org_id=1 WHERE id=2");
        String member = loginAs("admin-member", "123456");
        authRequest(member).get("/app/projects/1/items/order").then().body("code", equalTo(3));
        save(member, INITIAL, REORDERED).then().body("code", equalTo(3));
        page(member, 1, null).then().body("data.list.id", equalTo(INITIAL.subList(0, 4)));
        createUserWithOrganization(3L, "outsider", "123456", "outsider@example.com", 3L, 2L, "ORG_OWNER");
        String outsider = loginAs("outsider", "123456");
        authRequest(outsider).get("/app/projects/1/items/order").then().body("code", equalTo(6));
        save(outsider, INITIAL, REORDERED).then().body("code", equalTo(6));
        request().get("/app/projects/1/items/order").then().body("code", equalTo(1));
        request().body(Map.of("expectedItemIds", INITIAL, "itemIds", REORDERED))
                .put("/app/projects/1/items/order").then().body("code", equalTo(1));
        useRequestOrg(1L);
        jdbcTemplate.update("UPDATE project SET resource_status=0 WHERE id=1");
        authRequest(token).get("/app/projects/1/items/order").then().body("code", equalTo(6));
        save(token, INITIAL, REORDERED).then().body("code", equalTo(6));
        assertThat(databaseOrder()).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    void shouldRequireACallerTransactionForTheSharedParentLock() {
        seed();
        assertThatThrownBy(() -> projectService.lockEnabledProject(new ProjectAccessParamsBO(1L, 1L)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void shouldSerializeConcurrentCreationsAtTheTail() throws Exception {
        seed();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = workers.submit(() -> createAfterBarrier("G", ready, start));
            var second = workers.submit(() -> createAfterBarrier("H", ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertThat(jdbcTemplate.queryForList("SELECT sort FROM item WHERE title IN ('G','H') ORDER BY sort", Long.class))
                .containsExactly(7L, 8L);
    }

    @Test
    void shouldSerializeReorderingBeforeConcurrentCreation() throws Exception {
        seed();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch creating = new CountDownLatch(1);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var reorder = workers.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                projectService.lockEnabledProject(new ProjectAccessParamsBO(1L, 1L));
                locked.countDown();
                await(creating);
                itemService.reorderProjectItems(reorderParams());
            }));
            var create = workers.submit(() -> {
                await(locked);
                creating.countDown();
                itemService.createProjectItem(createParams("G"));
            });
            reorder.get(20, TimeUnit.SECONDS);
            create.get(20, TimeUnit.SECONDS);
        }
        assertThat(databaseOrder().subList(0, 6)).containsExactly(1L, 5L, 2L, 3L, 4L, 6L);
        assertThat(jdbcTemplate.queryForObject("SELECT sort FROM item WHERE title='G'", Long.class)).isEqualTo(7L);
    }

    @Test
    void shouldSerializeReviewDeletionAfterReordering() throws Exception {
        seed();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch deleting = new CountDownLatch(1);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var reorder = workers.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                projectService.lockEnabledProject(new ProjectAccessParamsBO(1L, 1L));
                locked.countDown();
                await(deleting);
                itemService.reorderProjectItems(reorderParams());
            }));
            var deletion = workers.submit(() -> {
                await(locked);
                deleting.countDown();
                itemService.deleteProjectItemForReview(1L, 2L, 1L);
            });
            reorder.get(20, TimeUnit.SECONDS);
            deletion.get(20, TimeUnit.SECONDS);
        }
        assertThat(databaseOrder()).containsExactly(1L, 5L, 3L, 4L, 6L);
        assertThat(jdbcTemplate.queryForObject("SELECT sort FROM item WHERE id=3", Long.class)).isEqualTo(4L);
    }

    private void createAfterBarrier(String title, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        await(start);
        itemService.createProjectItem(createParams(title));
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("同步栅栏超时");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }

    private ItemCreateParamsBO createParams(String title) {
        return ItemCreateParamsBO.builder().projectId(1L).orgId(1L).creatorId(1L)
                .title(title).taskTemplateId(1L).build();
    }

    private ItemReorderParamsBO reorderParams() {
        return ItemReorderParamsBO.builder().projectId(1L).orgId(1L).operatorId(1L)
                .expectedItemIds(INITIAL.stream().map(Long::valueOf).toList())
                .itemIds(REORDERED.stream().map(Long::valueOf).toList()).build();
    }

    private Response page(String token, int page, String sort) {
        var request = authRequest(token).queryParam("pageNum", page).queryParam("pageSize", 4);
        if (sort != null) request.queryParam("sortBy", sort);
        return request.get("/app/projects/1/items");
    }

    private Response save(String token, List<String> expected, List<String> requested) {
        return authRequest(token).body(Map.of("expectedItemIds", expected, "itemIds", requested))
                .put("/app/projects/1/items/order");
    }

    private List<Long> databaseOrder() {
        return jdbcTemplate.queryForList("SELECT id FROM item WHERE project_id=1 AND deleted_at IS NULL ORDER BY sort,id", Long.class);
    }

    private String seed() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_OWNER");
        jdbcTemplate.update("INSERT INTO project_type (id,org_id,name,creator_id) VALUES (1,1,'类型',1)");
        jdbcTemplate.update("INSERT INTO project (id,org_id,type_id,title,creator_id) VALUES (1,1,1,'排序企划',1)");
        jdbcTemplate.update("INSERT INTO task_template (id,org_id,name,scope,creator_id,ref_count) VALUES (1,1,'空模板',2,1,6)");
        for (int index = 1; index <= 6; index++) {
            jdbcTemplate.update("INSERT INTO item (id,project_id,title,sort,task_template_id,creator_id,created_at,updated_at) VALUES (?,1,?,?,1,1,?,?)",
                    index, String.valueOf((char) ('A' + index - 1)), index,
                    LocalDateTime.of(2026, 1, index, 0, 0), LocalDateTime.of(2026, 2, 1, 0, 0));
        }
        return loginAs("owner", "123456");
    }
}
