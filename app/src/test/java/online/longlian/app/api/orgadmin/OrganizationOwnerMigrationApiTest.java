package online.longlian.app.api.orgadmin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.api.util.DatabaseCleanupUtil;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.equalTo;

class OrganizationOwnerMigrationApiTest extends BaseApiTest {
    @Autowired private DatabaseCleanupUtil database;
    @Autowired private PlatformTransactionManager transactions;

    /** 历史创建者获得所有者权限，原 Token 下一次请求即能执行管理员治理。 */
    @Test
    void shouldBackfillEnabledCreatorAndAuthorizeExistingToken() {
        prepareFounder();
        String token = loginAs("founder", "123456");
        migrate();
        assertThat(role(1L)).isEqualTo("ORG_OWNER");
        assertThat(role(2L)).isEqualTo("ORG_ADMIN");
        assertThat(jdbcTemplate.queryForObject("SELECT creator_id FROM organization WHERE id=1", Long.class)).isEqualTo(1L);
        authRequest(token, 1L).body(Map.of("orgRole", "ORG_USER")).patch("/orgadmin/members/2/role")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(role(2L)).isEqualTo("ORG_USER");
    }

    /** 已转让过所有权的组织保留现任所有者，不把所有权还给审计创建人。 */
    @Test
    void shouldPreserveExistingTransferredOwner() {
        prepareFounder();
        jdbcTemplate.update("UPDATE organization_member SET org_role='ORG_OWNER' WHERE id=2");
        migrate();
        assertThat(role(1L)).isEqualTo("ORG_ADMIN");
        assertThat(role(2L)).isEqualTo("ORG_OWNER");
    }

    /** 异常数据阻断回填和成功记录，不自动选取其他管理员，也不部分修改有效组织。 */
    @ParameterizedTest
    @ValueSource(strings = {"missing-creator", "disabled-member", "disabled-user", "duplicate-owners", "invalid-role", "duplicate-member"})
    void shouldBlockInvalidLegacyOwnershipWithoutCommittingChanges(String state) {
        prepareFounder();
        switch (state) {
            case "missing-creator" -> jdbcTemplate.update("DELETE FROM organization_member WHERE id=1");
            case "disabled-member" -> jdbcTemplate.update("UPDATE organization_member SET status=0 WHERE id=1");
            case "disabled-user" -> jdbcTemplate.update("UPDATE user SET status=0 WHERE id=1");
            case "duplicate-owners" -> jdbcTemplate.update("UPDATE organization_member SET org_role='ORG_OWNER'");
            case "invalid-role" -> jdbcTemplate.update("UPDATE organization_member SET org_role='org_owner' WHERE id=1");
            case "duplicate-member" -> jdbcTemplate.update("INSERT INTO organization_member(id,org_id,user_id,org_role) VALUES(3,1,1,'ORG_USER')");
            default -> throw new IllegalArgumentException(state);
        }
        var before = jdbcTemplate.queryForList("SELECT id,org_role,status FROM organization_member ORDER BY id");
        Throwable failure = catchThrowable(this::migrate);
        assertThat(failure).isInstanceOf(ScriptStatementFailedException.class);
        assertThat(failure.getCause()).isInstanceOfSatisfying(SQLException.class,
                error -> assertThat(error.getSQLState()).isEqualTo("45000"));
        assertThat(jdbcTemplate.queryForList("SELECT id,org_role,status FROM organization_member ORDER BY id")).isEqualTo(before);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM data_migration", Integer.class)).isZero();
    }

    private void prepareFounder() {
        createUserWithOrganization(1L, "founder", "123456", "founder@example.com", 1L, 1L, "ORG_ADMIN");
        jdbcTemplate.update("UPDATE organization SET creator_id=1 WHERE id=1");
        createTestUser(2L, "admin", "123456", "admin@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_ADMIN");
    }

    private void migrate() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(
                        database.findSchemaFile().getParent().resolve("data-migrations/organization_owner.sql")));
                return null;
            });
            jdbcTemplate.update("INSERT INTO data_migration(filename) VALUES('organization_owner.sql')");
        });
    }

    private String role(Long id) {
        return jdbcTemplate.queryForObject("SELECT org_role FROM organization_member WHERE id=?", String.class, id);
    }
}
