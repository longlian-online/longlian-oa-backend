package online.longlian.app.api.open;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * 开放面企划只读接口的端到端验证：详情、成员键集、类型、项目键集、附件与按次读取链接。
 */
public class OpenProjectReadApiTest extends OpenApiFoundationApiTestSupport {

    private static final long USER_ID = 24001L;
    private static final long ORG_ID = 25000L;
    private static final long MEMBER_ID = 26000L;

    private String createKey() {
        return createKeyForUser(USER_ID, "openproject", MEMBER_ID, ORG_ID);
    }

    private void insertType(long typeId, String name, int status) {
        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, priority, status, creator_id) VALUES (?, ?, ?, 0, ?, ?)",
                typeId, ORG_ID, name, status, USER_ID);
    }

    private void insertProject(long id, String title, long creatorId, long orgId,
                               long typeId, String updatedAt) {
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, status, resource_status, creator_id, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, 1, 1, ?, NOW(), ?)",
                id, orgId, typeId, title, creatorId, updatedAt);
    }

    private void insertItem(long id, long projectId, String title, String updatedAt) {
        jdbcTemplate.update(
                "INSERT INTO `item` (id, project_id, title, sort, task_template_id, creator_id, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, 0, 0, ?, 1, NOW(), ?)",
                id, projectId, title, USER_ID, updatedAt);
    }

    private void insertWorkshop(long id, long projectId, long userId, String createdAt) {
        jdbcTemplate.update(
                "INSERT INTO `project_workshop` (id, project_id, user_id, created_at) VALUES (?, ?, ?, ?)",
                id, projectId, userId, createdAt);
    }

    private void insertMemberUser(long userId, long orgMemberId, String username) {
        createTestUser(userId, username, "123456", username + "@example.com");
        createOrganizationMember(orgMemberId, ORG_ID, userId, "ORG_USER");
    }

    private long insertAttachmentResource(long resourceId, long projectId) {
        jdbcTemplate.update(
                "INSERT INTO `resource` (id, org_id, storage_type, storage_key, file_name, file_ext, file_size, file_mime, "
                        + "biz_type, biz_id, process_status, creator_id, created_at, updated_at) "
                        + "VALUES (?, ?, 1, ?, '资料.zip', 'zip', 2048, 'application/zip', 'project_attachment', ?, 1, ?, NOW(), NOW())",
                resourceId, ORG_ID, "project_attachment/" + resourceId + ".zip", projectId, USER_ID);
        return resourceId;
    }

    @Test
    public void shouldGetProjectDetailWithSignedCover() {
        String key = createKey();
        insertType(40001L, "漫画", 1);
        long coverFileId = insertAttachmentResource(40010L, 0L);
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, description, cover_file_id, status, resource_status, creator_id, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1, ?, NOW(), '2026-01-02 08:00:00')",
                41000L, ORG_ID, 40001L, "开放企划", "别名", "简介", coverFileId, USER_ID);

        given().header("X-Api-Key", key).get("/open/v1/projects/41000").then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", equalTo(41000))
                .body("data.title", equalTo("开放企划"))
                .body("data.alias", equalTo("别名"))
                .body("data.typeId", equalTo(40001))
                .body("data.description", equalTo("简介"))
                .body("data.coverUrl", notNullValue())
                .body("data.thumbnailUrl", nullValue())
                .body("data.creator", equalTo(true))
                .body("data.updatedAt", notNullValue());

        // 详情不含管理视角与统计字段
        String body = given().header("X-Api-Key", key).get("/open/v1/projects/41000").body().asString();
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("canManage").doesNotContain("inWorkshop").doesNotContain("progressPercent");
    }

    @Test
    public void shouldReturnNotFoundForOtherOrgProjectDetail() {
        String key = createKey();
        insertProject(41001L, "别人的企划", USER_ID, ORG_ID + 1, 0, "2026-01-01 08:00:00");

        given().header("X-Api-Key", key).get("/open/v1/projects/41001").then()
                .statusCode(200)
                .body("code", equalTo(6));
    }

    @Test
    public void shouldPageMembersByUserIdCursor() {
        String key = createKey();
        insertProject(41002L, "成员企划", USER_ID, ORG_ID, 0, "2026-01-01 08:00:00");
        insertMemberUser(44001L, 44101L, "member_a");
        insertMemberUser(44002L, 44102L, "member_b");
        insertMemberUser(44003L, 44103L, "member_c");
        insertWorkshop(44201L, 41002L, USER_ID, "2026-01-01 09:00:00");
        insertWorkshop(44202L, 41002L, 44002L, "2026-01-02 09:00:00");
        insertWorkshop(44203L, 41002L, 44003L, "2026-01-03 09:00:00");

        Response page1 = given().header("X-Api-Key", key)
                .queryParam("limit", 2)
                .get("/open/v1/projects/41002/members");
        page1.then()
                .statusCode(200)
                .body("data.list", hasSize(2))
                .body("data.list[0].userId", equalTo((int) USER_ID))
                .body("data.list[0].creator", equalTo(true))
                .body("data.list[1].userId", equalTo(44002))
                .body("data.hasMore", equalTo(true))
                .body("data.nextCursor", notNullValue())
                .body("data.list[1].joinedAt", notNullValue())
                .body("data.list[1].completedCount", equalTo(0));
        // 密钥用户是企划创建人，首页按 userId 升序排最前
        org.assertj.core.api.Assertions.assertThat(page1.body().asString()).doesNotContain("avatarUrl");

        String cursor = page1.jsonPath().getString("data.nextCursor");
        given().header("X-Api-Key", key)
                .queryParam("limit", 2)
                .queryParam("cursor", cursor)
                .get("/open/v1/projects/41002/members").then()
                .statusCode(200)
                .body("data.list", hasSize(1))
                .body("data.list[0].userId", equalTo(44003))
                .body("data.list[0].creator", equalTo(false))
                .body("data.hasMore", equalTo(false))
                .body("data.nextCursor", nullValue());
    }

    @Test
    public void shouldListEnabledProjectTypesOnly() {
        String key = createKey();
        insertType(40002L, "小说", 1);
        insertType(40003L, "视频", 0);

        given().header("X-Api-Key", key).get("/open/v1/projects/types").then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasSize(1))
                .body("data[0].id", equalTo(40002))
                .body("data[0].name", equalTo("小说"));
    }

    @Test
    public void shouldPageItemsByUpdatedAtCursorWithProgress() {
        String key = createKey();
        insertProject(41003L, "项目企划", USER_ID, ORG_ID, 0, "2026-01-01 08:00:00");
        insertItem(42001L, 41003L, "项目一", "2026-01-01 09:00:00");
        insertItem(42002L, 41003L, "项目二", "2026-01-02 09:00:00");
        insertItem(42003L, 41003L, "项目三", "2026-01-03 09:00:00");

        Response page1 = given().header("X-Api-Key", key)
                .queryParam("limit", 2)
                .get("/open/v1/projects/41003/items");
        page1.then()
                .statusCode(200)
                .body("data.list", hasSize(2))
                .body("data.list[0].id", equalTo(42001))
                .body("data.list[0].status", equalTo("进行中"))
                .body("data.list[0].progressPercent", equalTo(0))
                .body("data.list[0].hasSubmissionRecord", equalTo(false))
                .body("data.hasMore", equalTo(true));

        // 增量过滤：只返回 updated_at 起点之后的项目
        given().header("X-Api-Key", key)
                .queryParam("updatedSince", "2026-01-03 00:00:00")
                .get("/open/v1/projects/41003/items").then()
                .statusCode(200)
                .body("data.list", hasSize(1))
                .body("data.list[0].id", equalTo(42003));

        String cursor = page1.jsonPath().getString("data.nextCursor");
        given().header("X-Api-Key", key)
                .queryParam("limit", 2)
                .queryParam("cursor", cursor)
                .get("/open/v1/projects/41003/items").then()
                .statusCode(200)
                .body("data.list", hasSize(1))
                .body("data.list[0].id", equalTo(42003))
                .body("data.hasMore", equalTo(false));

        // 项目列表不暴露节点结构
        org.assertj.core.api.Assertions.assertThat(page1.body().asString()).doesNotContain("nodes");
    }

    @Test
    public void shouldReturnNotFoundForOtherOrgItems() {
        String key = createKey();
        insertProject(41004L, "别组项目", USER_ID, ORG_ID + 1, 0, "2026-01-01 08:00:00");

        given().header("X-Api-Key", key).get("/open/v1/projects/41004/items").then()
                .statusCode(200)
                .body("code", equalTo(6));
    }

    @Test
    public void shouldListProjectAttachmentsWithoutReadUrl() {
        String key = createKey();
        insertProject(41005L, "附件企划", USER_ID, ORG_ID, 0, "2026-01-01 08:00:00");
        long fileId = insertAttachmentResource(40011L, 41005L);
        jdbcTemplate.update(
                "INSERT INTO `project_attachment` (id, project_id, file_id, uploader_id) VALUES (?, ?, ?, ?)",
                40021L, 41005L, fileId, USER_ID);

        Response list = given().header("X-Api-Key", key).get("/open/v1/projects/41005/attachments");
        list.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasSize(1))
                .body("data[0].fileId", equalTo((int) fileId))
                .body("data[0].name", equalTo("资料.zip"))
                .body("data[0].size", equalTo(2048))
                .body("data[0].mime", equalTo("application/zip"))
                .body("data[0].uploaderId", equalTo((int) USER_ID))
                .body("data[0].uploaderName", notNullValue());
        org.assertj.core.api.Assertions.assertThat(list.body().asString()).doesNotContain("readUrl");
    }

    @Test
    public void shouldIssueProjectAttachmentReadUrlPerCall() {
        String key = createKey();
        insertProject(41006L, "链接企划", USER_ID, ORG_ID, 0, "2026-01-01 08:00:00");
        long fileId = insertAttachmentResource(40012L, 41006L);
        jdbcTemplate.update(
                "INSERT INTO `project_attachment` (id, project_id, file_id, uploader_id) VALUES (?, ?, ?, ?)",
                40022L, 41006L, fileId, USER_ID);

        given().header("X-Api-Key", key)
                .get("/open/v1/projects/41006/attachments/40022/read-url").then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.readUrl", notNullValue())
                .body("data.expireAt", notNullValue());

        // 不存在的附件：不泄露存在性
        given().header("X-Api-Key", key)
                .get("/open/v1/projects/41006/attachments/40099/read-url").then()
                .statusCode(200)
                .body("code", equalTo(6));
    }

    @Test
    public void shouldRejectCrossEndpointCursor() {
        String key = createKey();
        insertProject(41007L, "游标企划", USER_ID, ORG_ID, 0, "2026-01-01 08:00:00");
        // 两名成员保证 limit=1 时能取到带 nextCursor 的页
        insertMemberUser(44004L, 44104L, "member_d");
        insertWorkshop(44204L, 41007L, USER_ID, "2026-01-01 09:00:00");
        insertWorkshop(44205L, 41007L, 44004L, "2026-01-02 09:00:00");
        // 成员游标没有时间分量，用在项目列表上按参数错误拒绝
        String memberCursor = given().header("X-Api-Key", key)
                .queryParam("limit", 1)
                .get("/open/v1/projects/41007/members").jsonPath().getString("data.nextCursor");
        org.assertj.core.api.Assertions.assertThat(memberCursor).isNotNull();
        given().header("X-Api-Key", key)
                .queryParam("cursor", memberCursor)
                .get("/open/v1/projects/41007/items").then()
                .statusCode(200)
                .body("code", equalTo(2));
    }
}
