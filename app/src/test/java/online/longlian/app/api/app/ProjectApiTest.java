package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.ProjectStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

public class ProjectApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 分页查询企划列表成功
     */
    @Test
    void shouldListProjectsSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", notNullValue())
                .body("data.total", notNullValue());
    }

    @Test
    void shouldResizeOnlyListCoversAndKeepDetailAndAvatarsOriginal() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createResource(1L, 1L, 1L);
        createResource(2L, 1L, 1L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'cover', storage_key = 'cover/1.png', biz_id = 1 WHERE id = 1");
        jdbcTemplate.update("UPDATE `user` SET avatar_file_id = 2 WHERE id = 1");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, creator_id) VALUES (1, 1, '漫画', 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, cover_file_id, creator_id) " +
                "VALUES (1, 1, 1, '有封面企划', 1, 1), (2, 1, 1, '无封面企划', 0, 1)");
        String token = loginAs("orgadmin", "123456");

        Response list = authRequest(token).get("/app/projects");
        list.then().statusCode(200).body("code", equalTo(0));
        String coverUrl = list.path("data.list.find { it.id == '1' }.coverUrl");
        assertThat(coverUrl).contains("&eo-img.resize=w/400/h/600");
        String avatarUrl = list.path("data.list.find { it.id == '1' }.creatorAvatarUrl");
        assertThat(avatarUrl).doesNotContain("eo-img.resize");
        Object absentCover = list.path("data.list.find { it.id == '2' }.coverUrl");
        assertThat(absentCover).isNull();

        Response detail = authRequest(token).get("/app/projects/1");
        detail.then().statusCode(200).body("code", equalTo(0));
        String originalCoverUrl = detail.path("data.coverUrl");
        assertThat(originalCoverUrl).contains("key=cover/1.png").doesNotContain("eo-img.resize");
    }

    /**
     * 按关键词筛选企划列表成功
     */
    @Test
    void shouldListProjectsWithKeywordFilter() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{\"tags\":[]}", 0L, "测试描述", 1, 1L
        );

        Response response = authRequest(token)
                .queryParam("keyword", "测试")
                .get("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", notNullValue());
    }

    /**
     * 按启用的企划类型名称筛选企划列表成功
     */
    @Test
    void shouldListProjectsWithProjectTypeFilter() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update("INSERT INTO `project_type` (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "漫画", 1, 1L);
        jdbcTemplate.update("INSERT INTO `project` (id, org_id, type_id, title, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, 1L, "漫画企划", 1L);

        authRequest(token)
                .queryParam("projectType", "  漫画  ")
                .get("/app/projects")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(1))
                .body("data.list[0].projectType", equalTo("漫画"));
    }

    /**
     * 按已禁用的企划类型名称筛选时应返回参数错误
     */
    @Test
    void shouldFailListProjectsWithDisabledProjectType() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update("INSERT INTO `project_type` (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "已停用", 0, 1L);

        authRequest(token)
                .queryParam("projectType", "已停用")
                .get("/app/projects")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 显式 offset 窗口应按绝对偏移取数，窗口间无缝衔接（首页少取后跨页不漏数据）
     */
    @Test
    void shouldListProjectsWithExplicitOffsetWindow() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                1L, 1L, 1L, "窗口企划甲", "offset-a", "{}", 0L, "偏移测试", 1, 1L,
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusMinutes(30))
        );
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                2L, 1L, 1L, "窗口企划乙", "offset-b", "{}", 0L, "偏移测试", 1, 1L,
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusMinutes(20))
        );
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                3L, 1L, 1L, "窗口企划丙", "offset-c", "{}", 0L, "偏移测试", 1, 1L,
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusMinutes(10))
        );

        // 常规首页：第一条为最新创建的丙
        authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 1)
                .get("/app/projects")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list[0].title", equalTo("窗口企划丙"));

        // 显式 offset=1：应取到乙，与首页无缝衔接
        authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 1)
                .queryParam("offset", 1)
                .get("/app/projects")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list[0].title", equalTo("窗口企划乙"))
                .body("data.total", equalTo(3));

        // 显式 offset=2：应取到甲
        authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 1)
                .queryParam("offset", 2)
                .get("/app/projects")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list[0].title", equalTo("窗口企划甲"));

        // 变长首页场景（首页少取、次页自接续位置连取）：offset=1&pageSize=2 应连取乙、甲
        authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 2)
                .queryParam("offset", 1)
                .get("/app/projects")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list[0].title", equalTo("窗口企划乙"))
                .body("data.list[1].title", equalTo("窗口企划甲"));
    }

    /**
     * offset 为负数时应返回参数错误
     */
    @Test
    void shouldFailListProjectsWithNegativeOffset() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 8)
                .queryParam("offset", -1)
                .get("/app/projects")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 使用不存在的企划类型名称筛选时应返回参数错误
     */
    @Test
    void shouldFailListProjectsWithUnknownProjectType() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .queryParam("projectType", "不存在的类型")
                .get("/app/projects")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 获取企划详情成功
     */
    @Test
    void shouldGetProjectDetailSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{\"tags\":[]}", 0L, "测试描述", 1, 1L,
                "2026-03-01 08:30:00"
        );

        Response response = authRequest(token)
                .get("/app/projects/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.title", notNullValue())
                .body("data.createdAt", equalTo("2026-03-01 08:30:00"));
    }

    /**
     * 获取企划类型列表成功
     */
    @Test
    void shouldGetProjectTypesSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );
        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                2L, 1L, "禁用类型", 0, 1L
        );

        Response response = authRequest(token)
                .get("/app/projects/types");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasSize(1))
                .body("data[0].name", equalTo("测试类型"));
    }

    /**
     * 创建企划成功，并默认加入创建者工坊
     */
    @Test
    void shouldCreateProjectSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createResource(1L, 1L, 1L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'cover', storage_key = 'cover/1.png', process_status = ? WHERE id = ?",
                FileProcessStatus.Uploaded.getCode(), 1L);
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "title", "测试企划",
                        "alias", "alias",
                        "typeId", "1",
                        "metadata", "{\"tags\":[]}",
                        "description", "测试描述",
                        "coverFileId", "1"
                ))
                .post("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        Long projectId = jdbcTemplate.queryForObject(
                "SELECT id FROM `project` WHERE title = ? AND creator_id = ?",
                Long.class, "测试企划", 1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `project_workshop` WHERE project_id = ? AND user_id = ?",
                Long.class, projectId, 1L)).isEqualTo(1L);

        authRequest(token)
                .get("/app/projects/" + projectId)
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.inWorkshop", equalTo(true));

        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/app/workshop/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(1))
                .body("data.list[0].title", equalTo("测试企划"));
    }

    /**
     * 创建时写入缩略图，编辑省略缩略图时保留，再次提交则替换。
     */
    @Test
    void shouldSaveAndReplaceProjectThumbnail() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createResource(1L, 1L, 1L);
        createResource(2L, 1L, 1L);
        createResource(3L, 1L, 1L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'cover', storage_key = 'cover/1.png', process_status = ? WHERE id = ?",
                FileProcessStatus.Uploaded.getCode(), 1L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'thumbnail', storage_key = 'thumbnail/2.png', process_status = ? WHERE id = ?",
                FileProcessStatus.Uploaded.getCode(), 2L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'thumbnail', storage_key = 'thumbnail/3.png', process_status = ? WHERE id = ?",
                FileProcessStatus.Uploaded.getCode(), 3L);
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L);

        authRequest(token)
                .body(Map.of(
                        "title", "带缩略图企划",
                        "alias", "alias",
                        "typeId", "1",
                        "metadata", "{\"tags\":[]}",
                        "description", "测试描述",
                        "coverFileId", "1",
                        "thumbnailFileId", "2"))
                .post("/app/projects")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        Long projectId = jdbcTemplate.queryForObject(
                "SELECT id FROM `project` WHERE title = ?", Long.class, "带缩略图企划");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT thumbnail_file_id FROM project WHERE id = ?", Long.class, projectId)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE id = 2", Integer.class))
                .isEqualTo(FileProcessStatus.Activated.getCode());

        authRequest(token)
                .get("/app/projects/" + projectId)
                .then()
                .statusCode(200)
                .body("data.thumbnailUrl", notNullValue());
        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/app/workshop/list")
                .then()
                .statusCode(200)
                .body("data.list[0].thumbnailUrl", notNullValue())
                .body("data.list[0].coverUrl", notNullValue());

        authRequest(token)
                .body(Map.of(
                        "title", "带缩略图企划",
                        "alias", "alias",
                        "metadata", "{\"tags\":[]}",
                        "description", "只改文字"))
                .put("/app/projects/" + projectId)
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT thumbnail_file_id FROM project WHERE id = ?", Long.class, projectId)).isEqualTo(2L);

        authRequest(token)
                .body(Map.of(
                        "title", "带缩略图企划",
                        "alias", "alias",
                        "metadata", "{\"tags\":[]}",
                        "description", "换缩略图",
                        "thumbnailFileId", "3"))
                .put("/app/projects/" + projectId)
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT thumbnail_file_id FROM project WHERE id = ?", Long.class, projectId)).isEqualTo(3L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT cover_file_id FROM project WHERE id = ?", Long.class, projectId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE id = 2", Integer.class))
                .isEqualTo(FileProcessStatus.Deprecated.getCode());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE id = 3", Integer.class))
                .isEqualTo(FileProcessStatus.Activated.getCode());
    }

    /**
     * 更新企划成功
     */
    @Test
    void shouldUpdateProjectSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createResource(1L, 1L, 1L);
        createResource(2L, 1L, 1L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'cover', storage_key = 'cover/2.png', process_status = ? WHERE id = ?",
                FileProcessStatus.Uploaded.getCode(), 2L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'cover', storage_key = 'cover/1.png', biz_id = 1, process_status = ? WHERE id = 1",
                FileProcessStatus.Activated.getCode());
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{\"tags\":[]}", 1L, "测试描述", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "title", "更新后的企划",
                        "alias", "new_alias",
                        "metadata", "{\"tags\":[{\"key\":\"key\",\"value\":\"value\"}]}",
                        "description", "更新后的描述",
                        "coverFileId", "2"
                ))
                .put("/app/projects/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE id = 1", Integer.class))
                .isEqualTo(FileProcessStatus.Deprecated.getCode());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE id = 2", Integer.class))
                .isEqualTo(FileProcessStatus.Activated.getCode());
    }

    /**
     * 编辑时未提交封面 ID，应更新文字字段并保留原封面资源。
     */
    @Test
    void shouldUpdateProjectWithoutCoverFileIdAndKeepExistingCover() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createResource(1L, 1L, 1L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'cover', storage_key = 'cover/1.png', biz_id = 1, process_status = ? WHERE id = 1",
                FileProcessStatus.Activated.getCode());
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L);
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{\"tags\":[]}", 1L, "测试描述", 1, 1L);

        authRequest(token)
                .body(Map.of(
                        "title", "更新后的企划",
                        "alias", "new_alias",
                        "metadata", "{\"tags\":[{\"key\":\"key\",\"value\":\"value\"}]}",
                        "description", "更新后的描述"))
                .put("/app/projects/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT title FROM project WHERE id = 1", String.class))
                .isEqualTo("更新后的企划");
        assertThat(jdbcTemplate.queryForObject("SELECT cover_file_id FROM project WHERE id = 1", Long.class))
                .isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id = 1", Integer.class))
                .isEqualTo(FileProcessStatus.Activated.getCode());
    }

    /**
     * 更新企划状态成功（进行中 → 已完成）
     */
    @Test
    void shouldUpdateProjectStatusSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createResource(1L, 1L, 1L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'cover', storage_key = 'cover/1.png', biz_id = 1, process_status = ? WHERE id = 1",
                FileProcessStatus.Activated.getCode());
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L);
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{\"tags\":[]}", 1L, "测试描述", 1, 1L);

        authRequest(token)
                .body(Map.of(
                        "title", "测试企划",
                        "alias", "alias",
                        "metadata", "{\"tags\":[]}",
                        "description", "测试描述",
                        "status", "已完成"))
                .put("/app/projects/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM project WHERE id = 1", Integer.class))
                .isEqualTo(ProjectStatus.COMPLETED.getCode());
        assertThat(jdbcTemplate.queryForObject("SELECT cover_file_id FROM project WHERE id = 1", Long.class))
                .isEqualTo(1L);
    }

    /**
     * 更新企划为已归档状态应失败，且其他字段不落库
     */
    @Test
    void shouldFailUpdateProjectStatusToArchived() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L);
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{\"tags\":[]}", 1L, "测试描述", 1, 1L);

        authRequest(token)
                .body(Map.of(
                        "title", "改名尝试",
                        "alias", "alias",
                        "metadata", "{\"tags\":[]}",
                        "description", "测试描述",
                        "status", "已归档"))
                .put("/app/projects/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM project WHERE id = 1", Integer.class))
                .isEqualTo(ProjectStatus.IN_PROGRESS.getCode());
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM project WHERE id = 1", String.class))
                .isEqualTo("测试企划");
    }

    /**
     * 添加企划到工坊成功
     */
    @Test
    void shouldAddProjectToWorkshopSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{\"tags\":[]}", 0L, "测试描述", 1, 1L
        );

        Response response = authRequest(token)
                .post("/app/projects/1/workshop");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 非负责人可以从工坊移除企划
     */
    @Test
    void shouldRemoveProjectFromWorkshopSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "member", "123456", "member@example.com");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 1L, 2L);
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String token = loginAs("member", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{\"tags\":[]}", 0L, "测试描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project_workshop` (id, project_id, user_id, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                1L, 1L, 2L
        );

        authRequest(token)
                .delete("/app/projects/1/workshop")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `project_workshop` WHERE id = 1 AND deleted_at IS NOT NULL",
                Integer.class)).isEqualTo(1);
    }

    /**
     * 企划负责人不能退出自己的企划
     */
    @Test
    void shouldRejectCreatorRemovingProjectFromWorkshop() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{\"tags\":[]}", 0L, "测试描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project_workshop` (id, project_id, user_id, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L
        );

        authRequest(token)
                .delete("/app/projects/1/workshop")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `project_workshop` WHERE id = 1 AND deleted_at IS NULL",
                Integer.class)).isEqualTo(1);
    }

    // ========== 认证失败 ==========

    /**
     * 未认证查询企划列表失败
     */
    @Test
    void shouldFailListProjectsWithoutAuth() {
        Response response = request()
                .get("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证获取企划详情失败
     */
    @Test
    void shouldFailGetProjectDetailWithoutAuth() {
        Response response = request()
                .get("/app/projects/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证获取企划类型失败
     */
    @Test
    void shouldFailGetProjectTypesWithoutAuth() {
        Response response = request()
                .get("/app/projects/types");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证创建企划失败
     */
    @Test
    void shouldFailCreateProjectWithoutAuth() {
        Response response = request()
                .body(Map.of("title", "test"))
                .post("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证更新企划失败
     */
    @Test
    void shouldFailUpdateProjectWithoutAuth() {
        Response response = request()
                .body(Map.of("title", "test"))
                .put("/app/projects/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证添加工坊失败
     */
    @Test
    void shouldFailAddWorkshopWithoutAuth() {
        Response response = request()
                .post("/app/projects/1/workshop");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证移除工坊失败
     */
    @Test
    void shouldFailRemoveWorkshopWithoutAuth() {
        Response response = request()
                .delete("/app/projects/1/workshop");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * Token无效查询企划列表失败
     */
    @Test
    void shouldFailListProjectsWithInvalidToken() {
        Response response = authRequest("invalid-token")
                .get("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 参数校验失败 ==========

    /**
     * 创建企划时标题为空应失败
     */
    @Test
    void shouldFailCreateProjectWithEmptyTitle() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("title", ""))
                .post("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 创建企划时别名为空应失败
     */
    @Test
    void shouldFailCreateProjectWithEmptyAlias() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "title", "企划",
                        "alias", "",
                        "typeId", "1",
                        "metadata", "{\"tags\":[]}",
                        "description", "描述",
                        "coverFileId", "1"
                ))
                .post("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 更新企划时标题为空应失败
     */
    @Test
    void shouldFailUpdateProjectWithEmptyTitle() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("title", ""))
                .put("/app/projects/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // ========== 业务规则失败 ==========

    /**
     * 获取不存在的企划详情应失败
     */
    @Test
    void shouldFailGetNonExistentProjectDetail() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/app/projects/99999");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 获取已禁用企划详情应失败
     */
    @Test
    void shouldFailGetDisabledProjectDetail() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, status, resource_status, creator_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, 1L, 1L, "禁用企划", 1, 0, 1L);

        authRequest(token)
                .get("/app/projects/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /**
     * 已禁用企划不能继续从用户端更新
     */
    @Test
    void shouldFailUpdateDisabledProject() {
        String token = createDisabledProjectAndLogin();

        authRequest(token)
                .body(Map.of(
                        "title", "更新后的企划",
                        "alias", "new_alias",
                        "metadata", "{\"tags\":[]}",
                        "description", "描述",
                        "coverFileId", "1"))
                .put("/app/projects/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /**
     * 已禁用企划不能继续加入用户工坊
     */
    @Test
    void shouldFailAddDisabledProjectToWorkshop() {
        String token = createDisabledProjectAndLogin();

        authRequest(token)
                .post("/app/projects/1/workshop")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /**
     * 已禁用企划不能继续从用户工坊移除
     */
    @Test
    void shouldFailRemoveDisabledProjectFromWorkshop() {
        String token = createDisabledProjectAndLogin();
        jdbcTemplate.update(
                "INSERT INTO project_workshop (id, project_id, user_id) VALUES (?, ?, ?)",
                1L, 1L, 1L);

        authRequest(token)
                .delete("/app/projects/1/workshop")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_workshop WHERE id = 1 AND deleted_at IS NULL", Integer.class))
                .isEqualTo(1);
    }

    /**
     * 更新不存在的企划应失败
     */
    @Test
    void shouldFailUpdateNonExistentProject() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "title", "更新后的企划",
                        "alias", "new_alias",
                        "metadata", "{\"tags\":[]}",
                        "description", "描述",
                        "coverFileId", "1"
                ))
                .put("/app/projects/99999");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    // ========== 边界条件 ==========

    /**
     * 查询企划列表时分页边界测试
     */
    @Test
    void shouldListProjectsWithPaginationBoundaries() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", 0)
                .queryParam("pageSize", 10)
                .get("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 查询企划列表时pageSize超过上限应失败
     */
    @Test
    void shouldListProjectsWithPageSizeExceedingMax() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 101)
                .get("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 查询企划列表时空数据应返回空列表
     */
    @Test
    void shouldListProjectsWithEmptyResult() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", hasSize(0))
                .body("data.total", equalTo(0));
    }

    private String createDisabledProjectAndLogin() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, status, resource_status, creator_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, 1L, 1L, "禁用企划", 1, 0, 1L);
        return loginAs("orgadmin", "123456");
    }

}
