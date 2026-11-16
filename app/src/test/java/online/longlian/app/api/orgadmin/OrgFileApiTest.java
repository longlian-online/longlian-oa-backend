package online.longlian.app.api.orgadmin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

@TestPropertySource(properties = {
        "storage.type=COS",
        "storage.cos.bucket=test-bucket",
        "storage.cos.region=ap-guangzhou",
        "storage.cos.secret-id=test-secret-id",
        "storage.cos.secret-key=test-secret-key",
        "storage.cdn.enabled=true",
        "storage.cdn.url-prefix=https://static.example.com",
        "storage.cdn.auth-key=test-cdn-key",
        "storage.cdn.auth-ttl-seconds=300",
        "storage.cdn.url-reuse-percent=80"
})
public class OrgFileApiTest extends BaseApiTest {

    private static final String SCHEMA = """
            [{"key":"script","label":"译稿","type":"file","required":true,"options":[]},\
            {"key":"note","label":"备注","type":"text","required":false,"options":[]}]""";

    @Test
    void shouldListProjectsItemsAndCurrentSubmissionFile() {
        String token = admin();
        insertProject(1L, 1L, "白企划");
        insertProject(2L, 1L, "阿企划");
        insertProject(3L, 1L, "停用企划");
        jdbcTemplate.update("UPDATE project SET resource_status = 0 WHERE id = 3");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, priority, status, creator_id) VALUES (7, 1, '漫画', 0, 1, 1)");
        jdbcTemplate.update("UPDATE project SET type_id = 7 WHERE org_id = 1");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, creator_id, deleted_at) VALUES (4, 1, 7, '已删企划', 1, NOW())");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (5, 2, 0, '外组织企划', 1)");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (11, 2, '第二话', 2, 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (10, 2, '第一话', 1, 1, 1)");
        insertNodeAndTask();
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, status) "
                        + "VALUES (1, 2, 10, 8, 7, 1, 1, ?, 2), (2, 2, 10, 8, 7, 1, 2, ?, 1)",
                "{\"script\":{\"fileId\":\"102\"},\"note\":\"旧\"}",
                "{\"script\":{\"fileId\":\"101\"},\"note\":\"新\"}");
        insertTaskResource(101L, 8L);
        insertTaskResource(102L, 8L);

        authRequest(token).get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1))
                .body("data.entries.name", equalTo(java.util.List.of("漫画")))
                .body("data.entries.kind", equalTo(java.util.List.of("FOLDER")));

        authRequest(token).queryParam("typeId", 7).get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.typeName", equalTo("漫画"))
                .body("data.total", equalTo(3))
                .body("data.entries.name", containsInAnyOrder("阿企划", "白企划", "停用企划"))
                .body("data.entries.kind", containsInAnyOrder("FOLDER", "FOLDER", "FOLDER"));

        authRequest(token).queryParam("projectId", 2).get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.projectTitle", equalTo("阿企划"))
                .body("data.total", equalTo(2))
                .body("data.entries.name", equalTo(java.util.List.of("第一话", "第二话")));

        authRequest(token).queryParam("projectId", 2).queryParam("itemId", 10).get("/orgadmin/files")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.itemTitle", equalTo("第一话"))
                .body("data.total", equalTo(2))
                .body("data.entries", hasSize(2))
                .body("data.entries[0].fileId", equalTo("101"))
                .body("data.entries[0].versionNo", equalTo(2))
                .body("data.entries[1].fileId", equalTo("102"))
                .body("data.entries[1].versionNo", equalTo(1))
                .body("data.entries[1].name", equalTo("成果102.pdf"))
                .body("data.entries[0].kind", equalTo("FILE"))
                .body("data.entries[0].name", equalTo("成果101.pdf"))
                .body("data.entries[0].taskName", equalTo("翻译"))
                .body("data.entries[0].fieldLabel", equalTo("译稿"))
                .body("data.entries[0].availability", equalTo("available"))
                .body("data.entries[0].submitterName", equalTo("orgadmin"))
                .body("data.entries[0].taskInstanceId", equalTo("8"));
    }

    @Test
    void shouldDownloadCurrentSubmissionFile() {
        String token = admin();
        insertProject(2L, 1L, "阿企划");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (10, 2, '第一话', 1, 1, 1)");
        insertNodeAndTask();
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, status) "
                        + "VALUES (2, 2, 10, 8, 7, 1, 1, ?, 1)",
                "{\"script\":{\"fileId\":\"101\"},\"note\":\"新\"}");
        insertTaskResource(101L, 8L);

        var download = authRequest(token).get("/orgadmin/files/tasks/8/attachments/101/download");
        download.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        URI uri = URI.create(download.jsonPath().getString("data.readUrl"));
        assertThat(uri.getHost()).isEqualTo("static.example.com");
        assertThat(uri.getRawPath()).isEqualTo("/task_submit/101.pdf");
    }

    @Test
    void shouldExpandFilesUnderAnItemWithoutNesting() {
        String token = admin();
        insertProject(2L, 1L, "阿企划");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (11, 2, '第二话', 2, 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (10, 2, '第一话', 1, 1, 1)");
        insertNodeAndTask();
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, status) "
                        + "VALUES (2, 2, 10, 8, 7, 1, 1, ?, 1)",
                "{\"script\":{\"fileId\":\"101\"},\"note\":\"新\"}");
        insertTaskResource(101L, 8L);

        var download = authRequest(token)
                .body(java.util.Map.of("targets", java.util.List.of(
                        java.util.Map.of("kind", "PROJECT", "projectId", "2"),
                        java.util.Map.of(
                                "kind", "FILE",
                                "taskInstanceId", "8",
                                "fileId", "101",
                                "fieldKey", "script"))))
                .post("/orgadmin/files/expansions");
        download.then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.files", hasSize(1))
                .body("data.files[0].taskInstanceId", equalTo("8"))
                .body("data.files[0].fieldKey", equalTo("script"))
                .body("data.files[0].fileId", equalTo("101"))
                .body("data.files[0].fileName", equalTo("成果101.pdf"))
                .body("data.files[0].projectTitle", equalTo("阿企划"))
                .body("data.files[0].itemTitle", equalTo("第一话"))
                .body("data.files[0].taskName", equalTo("翻译"))
                .body("data.files[0].fieldLabel", equalTo("译稿"))
                .body("data.files[0].versionNo", equalTo(1))
                .body("data.files[0].readUrl", nullValue());

        authRequest(token)
                .body(java.util.Map.of("targets", java.util.List.of(
                        java.util.Map.of("kind", "TASK", "projectId", "2", "taskId", "1"))))
                .post("/orgadmin/files/expansions")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.files", hasSize(1))
                .body("data.files[0].fileId", equalTo("101"));

        authRequest(token)
                .body(java.util.Map.of("targets", java.util.List.of(
                        java.util.Map.of(
                                "kind", "FILE",
                                "taskInstanceId", "8",
                                "fileId", "100",
                                "fieldKey", "script"))))
                .post("/orgadmin/files/expansions")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        authRequest(token)
                .body(java.util.Map.of("targets", java.util.List.of()))
                .post("/orgadmin/files/expansions")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailListFilesWithoutAuth() {
        request().get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldFailListFilesWithInvalidProjectId() {
        String token = admin();
        authRequest(token).queryParam("projectId", "abc").get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailWhenProjectOrItemDoesNotMatch() {
        String token = admin();
        insertProject(1L, 1L, "白企划");
        insertProject(2L, 1L, "阿企划");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (10, 2, '第一话', 1, 1, 1)");

        authRequest(token).queryParam("projectId", 9).get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        authRequest(token).queryParam("projectId", 1).queryParam("itemId", 10).get("/orgadmin/files")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()))
                .body("data", nullValue());
    }

    @Test
    void shouldBrowseByTypeAndByItem() {
        String token = admin();
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, priority, status, creator_id) VALUES (1, 1, '小说', 1, 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, priority, status, creator_id) VALUES (2, 1, '漫画', 5, 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, priority, status, creator_id) VALUES (3, 1, '停用类型', 5, 0, 1)");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, priority, status, creator_id) VALUES (4, 2, '外组织', 9, 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, priority, status, creator_id, deleted_at) VALUES (5, 1, '已删类型', 9, 1, 1, NOW())");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (1, 1, 2, '白企划', 1)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (2, 1, 2, '阿企划', 1)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (3, 1, 1, '小说企划', 1)");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (10, 2, '第一话', 1, 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (11, 1, '序章', 1, 1, 1)");
        insertNodeAndTask();
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, status) "
                        + "VALUES (2, 2, 10, 8, 7, 1, 1, ?, 1)",
                "{\"script\":{\"fileId\":\"101\"},\"note\":\"新\"}");
        insertTaskResource(101L, 8L);

        authRequest(token).get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(3))
                .body("data.entries.name", equalTo(java.util.List.of("停用类型", "漫画", "小说")));

        authRequest(token).queryParam("typeId", 2).get("/orgadmin/files")
                .then().statusCode(200)
                .body("data.typeId", equalTo("2"))
                .body("data.typeName", equalTo("漫画"))
                .body("data.projectId", nullValue())
                .body("data.entries.kind", equalTo(java.util.List.of("FOLDER", "FOLDER")))
                .body("data.entries.name", equalTo(java.util.List.of("白企划", "阿企划")));

        authRequest(token).queryParam("typeId", 1).get("/orgadmin/files")
                .then().statusCode(200)
                .body("data.typeName", equalTo("小说"))
                .body("data.total", equalTo(1))
                .body("data.entries.name", equalTo(java.util.List.of("小说企划")));

        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, status, creator_id) VALUES (20, 1, '停用任务', 0, 1)");
        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, status, creator_id, deleted_at) VALUES (21, 1, '已删任务', 1, 1, NOW())");
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, sort, parallel_sort) "
                        + "VALUES (12, 1, 10, 2, 20, '停用任务', 2, 1), (13, 1, 10, 2, 21, '已删任务', 3, 1)");

        authRequest(token).queryParam("typeId", 2).queryParam("projectId", 2).queryParam("view", "task")
                .get("/orgadmin/files")
                .then().statusCode(200)
                .body("data.entries.name", equalTo(java.util.List.of("翻译", "停用任务", "已删任务")))
                .body("data.entries.id", equalTo(java.util.List.of("1", "20", "21")));

        authRequest(token).queryParam("typeId", 2).queryParam("projectId", 2)
                .queryParam("view", "task").queryParam("taskId", 1)
                .get("/orgadmin/files")
                .then().statusCode(200)
                .body("data.taskName", equalTo("翻译"))
                .body("data.entries[0].kind", equalTo("FILE"))
                .body("data.entries[0].name", equalTo("成果101.pdf"))
                .body("data.entries[0].subtitle", equalTo("第一话"));

        authRequest(token).queryParam("typeId", 2).queryParam("projectId", 2).get("/orgadmin/files")
                .then().statusCode(200)
                .body("data.entries.name", equalTo(java.util.List.of("第一话")));

        authRequest(token).queryParam("view", "nope").get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailWhenMemberIsNotManager() {
        createUserWithOrganization(1L, "regular", "123456", "regular@example.com", 1L, 1L, "ORG_USER");
        authRequest(loginAs("regular", "123456")).get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    @Test
    void shouldFailDownloadForRejectedOrForeignFile() {
        String token = admin();
        insertProject(2L, 1L, "阿企划");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (10, 2, '第一话', 1, 1, 1)");
        insertNodeAndTask();
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, status) "
                        + "VALUES (1, 2, 10, 8, 7, 1, 1, ?, 2), (2, 2, 10, 8, 7, 1, 2, ?, 1)",
                "{\"script\":{\"fileId\":\"102\"}}",
                "{\"script\":{\"fileId\":\"101\"}}");
        insertTaskResource(101L, 8L);
        insertTaskResource(102L, 8L);
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (6, 2, 0, '外组织企划', 1)");
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, status) VALUES (9, 6, 10, 7, 1, 3)");

        authRequest(token).get("/orgadmin/files/tasks/8/attachments/102/download").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).get("/orgadmin/files/tasks/9/attachments/101/download").then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    @Test
    void shouldDiscardCurrentFileWhenNothingElseReferencesIt() {
        String token = admin();
        insertProject(2L, 1L, "阿企划");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (10, 2, '第一话', 1, 1, 1)");
        insertNodeAndTask();
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, status) "
                        + "VALUES (2, 2, 10, 8, 7, 1, 1, ?, 1)",
                "{\"script\":{\"fileId\":\"101\"},\"note\":\"新\"}");
        insertTaskResource(101L, 8L);

        authRequest(token).queryParam("fieldKey", "script")
                .delete("/orgadmin/files/tasks/8/attachments/101").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM task_submission WHERE id = 2", String.class))
                .contains("101");
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id = 101", Integer.class))
                .isEqualTo(FileProcessStatus.Deprecated.getCode());
        assertThat(jdbcTemplate.queryForObject("SELECT deprecated_at FROM resource WHERE id = 101", Object.class))
                .isNotNull();
        authRequest(token).queryParam("projectId", 2).queryParam("itemId", 10).get("/orgadmin/files")
                .then().body("data.entries", hasSize(0));
    }

    @Test
    void shouldFailDiscardWhenFileIsStillUsedAsCover() {
        String token = admin();
        insertProject(2L, 1L, "阿企划");
        jdbcTemplate.update("UPDATE project SET cover_file_id = 101 WHERE id = 2");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, sort, task_template_id, creator_id) VALUES (10, 2, '第一话', 1, 1, 1)");
        insertNodeAndTask();
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, status) "
                        + "VALUES (2, 2, 10, 8, 7, 1, 1, ?, 1)",
                "{\"script\":{\"fileId\":\"101\"}}");
        insertTaskResource(101L, 8L);

        authRequest(token).queryParam("fieldKey", "script")
                .delete("/orgadmin/files/tasks/8/attachments/101").then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id = 101", Integer.class))
                .isEqualTo(FileProcessStatus.Activated.getCode());
    }

    @Test
    void shouldFailDiscardWithoutFieldKey() {
        String token = admin();
        authRequest(token).delete("/orgadmin/files/tasks/8/attachments/101").then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldPageTypesAtTheRoot() {
        String token = admin();
        for (int index = 1; index <= 51; index++) {
            jdbcTemplate.update(
                    "INSERT INTO project_type (id, org_id, name, priority, status, creator_id) VALUES (?, 1, ?, 0, 1, 1)",
                    index, String.format("%03d", index));
        }

        authRequest(token).queryParam("pageNum", 2).get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(51))
                .body("data.entries", hasSize(1))
                .body("data.entries[0].name", equalTo("051"));
    }

    @Test
    void shouldReturnEmptyRootWhenOrganizationHasNoTypes() {
        String token = admin();
        authRequest(token).get("/orgadmin/files").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(0))
                .body("data.entries", hasSize(0))
                .body("data.projectId", nullValue());
    }

    private String admin() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        return loginAs("orgadmin", "123456");
    }

    private void insertProject(long id, long orgId, String title) {
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (?, ?, 0, ?, 1)",
                id, orgId, title);
    }

    private void insertNodeAndTask() {
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                        + "VALUES (7, 1, 10, 2, 1, '翻译', ?, 1, 1)",
                SCHEMA);
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status) "
                        + "VALUES (8, 2, 10, 7, 1, 1, 3)");
    }

    private void insertTaskResource(long id, long taskId) {
        jdbcTemplate.update(
                "INSERT INTO resource (id, org_id, storage_type, storage_key, file_name, file_ext, file_size, file_mime, biz_type, biz_id, process_status, creator_id) "
                        + "VALUES (?, 1, 3, ?, ?, 'pdf', 2048, 'application/pdf', 'task_submit', ?, ?, 1)",
                id, "task_submit/" + id + ".pdf", "成果" + id + ".pdf", taskId, FileProcessStatus.Activated.getCode());
    }
}
