package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ItemTaskFlowEditApiTest extends BaseApiTest {

    @Test
    void shouldReorderAddAndDeleteUntouchedNode() {
        String token = prepareCreator();
        Long itemId = createItem(token);
        List<Map<String, Object>> nodes = flowNodes(token, itemId);
        String keptId = stringField(nodes.get(0), "id");
        String removedId = stringField(nodes.get(1), "id");

        authRequest(token).body(Map.of(
                "nodes", List.of(
                        node(null, "2", 1, 1, "新校对"),
                        node(keptId, "1", 2, 1, "翻译改名")),
                "removed", List.of(Map.of("id", removedId, "revision", "0"))
        )).put("/app/item/" + itemId + "/flow").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        authRequest(token).get("/app/item/" + itemId + "/flow").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.nodes", hasSize(2))
                .body("data.nodes[0].name", equalTo("新校对"))
                .body("data.nodes[0].baseTaskId", equalTo("2"))
                .body("data.nodes[0].revision", equalTo("0"))
                .body("data.nodes[0].removable", equalTo(true))
                .body("data.nodes[1].id", equalTo(keptId))
                .body("data.nodes[1].name", equalTo("翻译改名"))
                .body("data.nodes[1].revision", equalTo("1"))
                .body("data.nodes[1].sort", equalTo(2));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT meta_schema FROM item_task_node WHERE item_id = ? AND base_task_id = 2 AND deleted_at IS NULL",
                String.class, itemId)).contains("note");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM item_task_node WHERE item_id = ? AND deleted_at IS NULL",
                Long.class, itemId)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task_instance WHERE item_id = ? AND deleted_at IS NULL",
                Long.class, itemId)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT ref_count FROM task_template WHERE id = 1", Integer.class)).isEqualTo(1);
    }

    @Test
    void shouldFailWhenCallerIsNotProjectCreator() {
        prepareCreator();
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String creator = loginAs("creator", "123456");
        String member = loginAs("member", "123456");
        Long itemId = createItem(creator);
        Map<String, Object> node = flowNodes(creator, itemId).get(0);

        authRequest(member).body(Map.of(
                "nodes", List.of(node(stringField(node, "id"), "1", 1, 1, "翻译")),
                "removed", List.of(Map.of(
                        "id", stringField(flowNodes(creator, itemId).get(1), "id"),
                        "revision", "0"))
        )).put("/app/item/" + itemId + "/flow").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    @Test
    void shouldFailWithoutAuth() {
        request().body(Map.of("nodes", List.of(node(null, "1", 1, 1, "翻译"))))
                .put("/app/item/1/flow").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldFailWhenNodeWasClaimedOrRevisionIsStale() {
        String token = prepareCreator();
        Long itemId = createItem(token);
        List<Map<String, Object>> nodes = flowNodes(token, itemId);
        String firstId = stringField(nodes.get(0), "id");
        String secondId = stringField(nodes.get(1), "id");
        jdbcTemplate.update(
                "UPDATE task_instance SET status = 2, assignee_id = 1, revision = 1 WHERE item_task_node_id = ?",
                Long.valueOf(secondId));

        authRequest(token).body(Map.of(
                "nodes", List.of(node(firstId, "1", 1, 1, "翻译")),
                "removed", List.of(Map.of("id", secondId, "revision", "0"))
        )).put("/app/item/" + itemId + "/flow").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", containsString("任务流已变更，请刷新后重试"));

        authRequest(token).body(Map.of(
                "nodes", List.of(node(firstId, "1", 1, 1, "翻译")),
                "removed", List.of(Map.of("id", secondId, "revision", "1"))
        )).put("/app/item/" + itemId + "/flow").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", containsString("已接取或已有提交的任务不能删除"));
    }

    @Test
    void shouldFailWhenChangingBaseTaskOrAddingDisabledTask() {
        String token = prepareCreator();
        Long itemId = createItem(token);
        List<Map<String, Object>> nodes = flowNodes(token, itemId);

        authRequest(token).body(Map.of("nodes", List.of(
                node(stringField(nodes.get(0), "id"), "2", 1, 1, "翻译"),
                node(stringField(nodes.get(1), "id"), "1", 2, 1, "校对")
        ))).put("/app/item/" + itemId + "/flow").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                .body("msg", containsString("不能更换已有任务的原子任务"));

        authRequest(token).body(Map.of("nodes", List.of(
                node(stringField(nodes.get(0), "id"), "1", 1, 1, "翻译"),
                node(stringField(nodes.get(1), "id"), "1", 2, 1, "校对"),
                node(null, "3", 3, 1, "停用")
        ))).put("/app/item/" + itemId + "/flow").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailWhenNodesAreEmpty() {
        String token = prepareCreator();
        Long itemId = createItem(token);

        authRequest(token).body(Map.of("nodes", List.of()))
                .put("/app/item/" + itemId + "/flow").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    private String prepareCreator() {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_USER");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '漫画', 1, 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) VALUES (1, 1, 1, '企划', 'alias', '{}', 0, '说明', 1, 1)");
        jdbcTemplate.update("INSERT INTO task_template (id, org_id, name, status, scope, creator_id, ref_count) VALUES (1, 1, '模板', 1, 1, 1, 0)");
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (1, 1, '翻译', '[]', 1, 1), (2, 1, '校对', '[{\"key\":\"note\",\"label\":\"备注\",\"type\":\"text\",\"required\":false,\"options\":[]}]', 1, 1), (3, 1, '停用', '[]', 0, 1)");
        jdbcTemplate.update("INSERT INTO task_template_node (id, task_template_id, base_task_id, sort, parallel_sort) VALUES (1, 1, 1, 1, 1), (2, 1, 1, 2, 1)");
        return loginAs("creator", "123456");
    }

    private Long createItem(String token) {
        authRequest(token).body(Map.of("title", "第1话", "taskTemplateId", "1"))
                .post("/app/projects/1/items").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        return jdbcTemplate.queryForObject("SELECT id FROM item WHERE title = '第1话'", Long.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> flowNodes(String token, Long itemId) {
        return authRequest(token).get("/app/item/" + itemId + "/flow")
                .then().statusCode(200).extract().path("data.nodes");
    }

    private static Map<String, Object> node(String id, String baseTaskId, int sort, int parallelSort, String name) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        if (id != null) {
            body.put("id", id);
            body.put("revision", "0");
        }
        body.put("baseTaskId", baseTaskId);
        body.put("sort", sort);
        body.put("parallelSort", parallelSort);
        body.put("name", name);
        return body;
    }

    private static String stringField(Map<String, Object> node, String field) {
        return String.valueOf(node.get(field));
    }
}
