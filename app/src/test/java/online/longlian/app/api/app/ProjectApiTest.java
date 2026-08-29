package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "测试描述", 1, 1L
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
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "测试描述", 1, 1L
        );

        Response response = authRequest(token)
                .get("/app/projects/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.title", notNullValue());
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
     * 创建企划成功
     */
    @Test
    void shouldCreateProjectSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createResource(1L, 1L, 1L);
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
                        "metadata", "{}",
                        "description", "测试描述",
                        "coverFileId", "1"
                ))
                .post("/app/projects");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 更新企划成功
     */
    @Test
    void shouldUpdateProjectSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createResource(2L, 1L, 1L);
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "测试描述", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "title", "更新后的企划",
                        "alias", "new_alias",
                        "metadata", "{\"key\":\"value\"}",
                        "description", "更新后的描述",
                        "coverFileId", "2"
                ))
                .put("/app/projects/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
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
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "测试描述", 1, 1L
        );

        Response response = authRequest(token)
                .post("/app/projects/1/workshop");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 从工坊移除企划成功
     */
    @Test
    void shouldRemoveProjectFromWorkshopSuccessfully() {
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
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "测试描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project_workshop` (id, project_id, user_id, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L
        );

        Response response = authRequest(token)
                .delete("/app/projects/1/workshop");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
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
                        "metadata", "{}",
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
                        "metadata", "{}",
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
}
