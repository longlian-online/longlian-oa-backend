package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.*;

public class OrgAdminProjectTypeApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 分页查询企划类型列表成功
     */
    @Test
    void shouldListProjectTypesSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/project-types?pageNum=1&pageSize=10");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", notNullValue())
                .body("data.total", notNullValue());
    }

    /**
     * 创建企划类型成功
     */
    @Test
    void shouldCreateProjectTypeSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("name", "测试企划类型"))
                .post("/orgadmin/project-types");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 修改企划类型名称成功，并自动去除首尾空格
     */
    @Test
    void shouldUpdateProjectTypeSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update("INSERT INTO `project_type` (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "旧名称", 1, 1L);

        authRequest(token).body(Map.of("name", "  新名称  "))
                .put("/orgadmin/project-types/1")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT name FROM project_type WHERE id = 1", String.class))
                .isEqualTo("新名称");
    }

    /**
     * 修改企划类型为组织内重复名称失败
     */
    @Test
    void shouldFailUpdateProjectTypeWithDuplicateName() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update("INSERT INTO `project_type` (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "类型一", 1, 1L);
        jdbcTemplate.update("INSERT INTO `project_type` (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                2L, 1L, "类型二", 1, 1L);

        authRequest(token).body(Map.of("name", "类型二"))
                .put("/orgadmin/project-types/1")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 删除未被引用的企划类型成功
     */
    @Test
    void shouldDeleteUnusedProjectTypeSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update("INSERT INTO `project_type` (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "待删除", 1, 1L);

        authRequest(token).delete("/orgadmin/project-types/1")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM project_type WHERE id = 1", LocalDateTime.class))
                .isNotNull();
    }

    /**
     * 删除已被企划引用的类型失败
     */
    @Test
    void shouldFailDeleteProjectTypeWhenReferenced() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update("INSERT INTO `project_type` (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "在用类型", 1, 1L);
        jdbcTemplate.update("INSERT INTO `project` (id, org_id, type_id, title, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, 1L, "企划", 1L);

        authRequest(token).delete("/orgadmin/project-types/1")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 未认证修改企划类型失败
     */
    @Test
    void shouldFailUpdateProjectTypeWithoutAuth() {
        request().body(Map.of("name", "新名称"))
                .put("/orgadmin/project-types/1")
                .then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证删除企划类型失败
     */
    @Test
    void shouldFailDeleteProjectTypeWithoutAuth() {
        request().delete("/orgadmin/project-types/1")
                .then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 修改企划类型名称为空时应失败
     */
    @Test
    void shouldFailUpdateProjectTypeWithEmptyName() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        authRequest(token).body(Map.of("name", ""))
                .put("/orgadmin/project-types/1")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 启用企划类型成功
     */
    @Test
    void shouldEnableProjectTypeSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试企划类型", 0, 1L
        );

        Response response = authRequest(token)
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/project-types/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 禁用企划类型成功
     */
    @Test
    void shouldDisableProjectTypeSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试企划类型", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/project-types/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证查询企划类型列表失败
     */
    @Test
    void shouldFailListProjectTypesWithoutAuth() {
        Response response = request()
                .get("/orgadmin/project-types?pageNum=1&pageSize=10");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证创建企划类型失败
     */
    @Test
    void shouldFailCreateProjectTypeWithoutAuth() {
        Response response = request()
                .body(Map.of("name", "test"))
                .post("/orgadmin/project-types");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证修改企划类型状态失败
     */
    @Test
    void shouldFailChangeProjectTypeStatusWithoutAuth() {
        Response response = request()
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/project-types/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 参数校验失败 ==========

    /**
     * 创建企划类型时名称为空应失败
     */
    @Test
    void shouldFailCreateProjectTypeWithEmptyName() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("name", ""))
                .post("/orgadmin/project-types");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 创建企划类型时名称超长应失败
     */
    @Test
    void shouldFailCreateProjectTypeWithNameTooLong() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        String longName = "a".repeat(51);
        Response response = authRequest(token)
                .body(Map.of("name", longName))
                .post("/orgadmin/project-types");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 修改企划类型状态时status为空应失败
     */
    @Test
    void shouldFailChangeProjectTypeStatusWithEmptyStatus() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", ""))
                .patch("/orgadmin/project-types/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // ========== 业务规则失败 ==========

    /**
     * 修改不存在的企划类型状态应失败
     */
    @Test
    void shouldFailChangeStatusForNonExistentProjectType() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/project-types/99999/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    // ========== 边界条件 ==========

    /**
     * 查询企划类型列表时分页边界测试
     */
    @Test
    void shouldListProjectTypesWithPaginationBoundaries() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/project-types?pageNum=0&pageSize=10");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 查询企划类型列表时pageSize超过上限应失败
     */
    @Test
    void shouldListProjectTypesWithPageSizeExceedingMax() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/project-types?pageNum=1&pageSize=101");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 查询企划类型列表时关键词筛选测试
     */
    @Test
    void shouldListProjectTypesWithKeywordFilter() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/project-types?pageNum=1&pageSize=10&keyword=测试");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 查询企划类型列表时空数据应返回空列表
     */
    @Test
    void shouldListProjectTypesWithEmptyResult() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/project-types?pageNum=1&pageSize=10");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(0))
                .body("data.total", equalTo(0));
    }

    // ========== 权限检查 ==========

    /**
     * Token无效查询企划类型列表失败
     */
    @Test
    void shouldFailListProjectTypesWithInvalidToken() {
        Response response = authRequest("invalid-token")
                .get("/orgadmin/project-types?pageNum=1&pageSize=10");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未授权角色查询企划类型列表应失败
     */
    @Test
    void shouldFailListProjectTypesWithoutAdminRole() {
        createUserWithOrganization(1L, "regular", "123456", "regular@example.com", 1L, 1L, "MEMBER");
        String token = loginAs("regular", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/project-types?pageNum=1&pageSize=10");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }
}
