package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

public class WorkshopApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 分页查询工坊企划列表成功
     */
    @Test
    void shouldListWorkshopProjectsSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/app/workshop/list");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", notNullValue())
                .body("data.total", notNullValue());
    }

    /**
     * 按关键词筛选工坊企划列表成功
     */
    @Test
    void shouldListWorkshopProjectsWithKeywordFilter() {
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
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project_workshop` (id, project_id, user_id, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "keyword", "测试"
                ))
                .post("/app/workshop/list");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", notNullValue());
    }

    /**
     * 按有效企划类型筛选工坊企划列表成功，并自动去除首尾空格
     */
    @Test
    void shouldListWorkshopProjectsWithProjectTypeFilter() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "漫画", 1, 1L);
        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                2L, 1L, "视频", 1, 1L);
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, 1L, "漫画企划", 1L);
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, creator_id) VALUES (?, ?, ?, ?, ?)",
                2L, 1L, 2L, "视频企划", 1L);
        jdbcTemplate.update(
                "INSERT INTO `project_workshop` (id, project_id, user_id) VALUES (?, ?, ?)",
                1L, 1L, 1L);
        jdbcTemplate.update(
                "INSERT INTO `project_workshop` (id, project_id, user_id) VALUES (?, ?, ?)",
                2L, 2L, 1L);

        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10, "projectType", "  漫画  "))
                .post("/app/workshop/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(1))
                .body("data.list[0].title", equalTo("漫画企划"))
                .body("data.total", equalTo(1));
    }

    /**
     * 使用不存在的企划类型筛选工坊时应返回参数错误
     */
    @Test
    void shouldFailListWorkshopProjectsWithUnknownProjectType() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10, "projectType", "不存在的类型"))
                .post("/app/workshop/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 使用已禁用的企划类型筛选工坊时应返回参数错误
     */
    @Test
    void shouldFailListWorkshopProjectsWithDisabledProjectType() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "已禁用类型", 0, 1L);

        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10, "projectType", "已禁用类型"))
                .post("/app/workshop/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 分页查询工坊任务流模板列表成功
     */
    @Test
    void shouldListWorkshopTaskTemplatesSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/app/workshop/task-template/list");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", notNullValue())
                .body("data.total", notNullValue());
    }

    /**
     * 查询工坊任务流模板时返回原子任务Lucide图标组件名
     */
    @Test
    void shouldListWorkshopTaskTemplatesWithBaseTaskIconName() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, icon_name, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "宣传", "描述", null, "Megaphone", "[]", 1, 1L
        );
        jdbcTemplate.update(
                "INSERT INTO `task_template` (id, org_id, name, description, status, scope, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "宣传模板", "描述", 1, 2, 1L
        );
        jdbcTemplate.update(
                "INSERT INTO `task_template_node` (id, task_template_id, base_task_id, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1, 0
        );

        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/app/workshop/task-template/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(1))
                .body("data.list[0].nodes[0].baseTaskIconName", equalTo("Megaphone"));
    }

    /**
     * 创建工坊个人任务流模板成功
     */
    @Test
    void shouldCreateWorkshopTaskTemplateSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务1", "描述", null, "[]", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "我的任务流模板",
                        "description", "模板描述",
                        "nodes", List.of(
                                Map.of("baseTaskId", "1", "sort", 0, "parallelSort", 1)
                        )
                ))
                .post("/app/workshop/task-template");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 更新工坊个人任务流模板成功
     */
    @Test
    void shouldUpdateWorkshopTaskTemplateSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务1", "描述", null, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `task_template` (id, org_id, name, description, status, scope, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "模板", "描述", 1, 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "更新后的模板名",
                        "description", "更新后的描述",
                        "nodes", List.of(
                                Map.of("baseTaskId", "1", "sort", 0, "parallelSort", 1)
                        )
                ))
                .put("/app/workshop/task-template/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证查询工坊企划列表失败
     */
    @Test
    void shouldFailListWorkshopWithoutAuth() {
        Response response = request()
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/app/workshop/list");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证查询工坊任务流模板列表失败
     */
    @Test
    void shouldFailListTaskTemplatesWithoutAuth() {
        Response response = request()
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/app/workshop/task-template/list");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证创建工坊任务流模板失败
     */
    @Test
    void shouldFailCreateTaskTemplateWithoutAuth() {
        Response response = request()
                .body(Map.of("name", "test"))
                .post("/app/workshop/task-template");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证更新工坊任务流模板失败
     */
    @Test
    void shouldFailUpdateTaskTemplateWithoutAuth() {
        Response response = request()
                .body(Map.of("name", "test"))
                .put("/app/workshop/task-template/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * Token无效查询工坊企划列表失败
     */
    @Test
    void shouldFailListWorkshopWithInvalidToken() {
        Response response = authRequest("invalid-token")
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/app/workshop/list");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 参数校验失败 ==========

    /**
     * 创建工坊任务流模板时名称为空应失败
     */
    @Test
    void shouldFailCreateTaskTemplateWithEmptyName() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("name", ""))
                .post("/app/workshop/task-template");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 创建工坊任务流模板时名称超长应失败
     */
    @Test
    void shouldFailCreateTaskTemplateWithNameTooLong() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        String longName = "a".repeat(101);
        Response response = authRequest(token)
                .body(Map.of("name", longName))
                .post("/app/workshop/task-template");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 创建工坊任务流模板时节点列表为空应失败
     */
    @Test
    void shouldFailCreateTaskTemplateWithEmptyNodes() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "模板",
                        "nodes", List.of()
                ))
                .post("/app/workshop/task-template");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // ========== 业务规则失败 ==========

    /**
     * 更新不存在的工坊任务流模板应失败
     */
    @Test
    void shouldFailUpdateNonExistentTaskTemplate() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务1", "描述", null, "[]", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "模板",
                        "nodes", List.of(
                                Map.of("baseTaskId", "1", "sort", 0, "parallelSort", 1)
                        )
                ))
                .put("/app/workshop/task-template/99999");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    // ========== 边界条件 ==========

    /**
     * 查询工坊企划列表时分页边界测试
     */
    @Test
    void shouldListWorkshopWithPaginationBoundaries() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 0, "pageSize", 10))
                .post("/app/workshop/list");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 查询工坊企划列表时空数据应返回空列表
     */
    @Test
    void shouldListWorkshopWithEmptyResult() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/app/workshop/list");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", hasSize(0))
                .body("data.total", equalTo(0));
    }
}
