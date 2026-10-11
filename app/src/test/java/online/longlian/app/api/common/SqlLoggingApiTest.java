package online.longlian.app.api.common;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {
        "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.slf4j.Slf4jImpl",
        "logging.level.online.longlian.app.mapper=INFO"
})
class SqlLoggingApiTest extends BaseApiTest {
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    /** 数据库请求在 INFO 级别下仍应成功，且不能输出 SQL 和绑定参数。 */
    @Test
    void shouldExecuteRequestsWithoutSqlOutput(CapturedOutput output) {
        createAdmin(1L, "admin", "123456", "root");
        String token = adminLoginAs("admin", "123456");
        authRequest(token).get("/admin/admins/").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[0].username", equalTo("admin"));
        assertThat(sqlSessionFactory.getConfiguration().getLogImpl()).isEqualTo(Slf4jImpl.class);
        assertThat(output.getAll()).doesNotContain("Preparing:", "Parameters:");
    }
}
