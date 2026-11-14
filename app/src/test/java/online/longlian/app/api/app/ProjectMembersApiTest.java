package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class ProjectMembersApiTest extends BaseApiTest {
    private static final String MEMBERS = "/app/projects/1/members";

    /** 当前成员可由普通用户查看，昵称为空回退用户名；没有有效提交时完成数为 0、时间为空。 */
    @Test
    void shouldListCurrentMembersWithPublicProfiles() {
        String token = prepareProject();
        addMember(2L);
        jdbcTemplate.update("UPDATE `user` SET nickname = '' WHERE id = 2");
        jdbcTemplate.update("UPDATE `user` SET nickname = '  昵称  ' WHERE id = 1");
        jdbcTemplate.update("INSERT INTO resource (id, org_id, storage_type, storage_key, file_name, file_ext, file_size, biz_type, biz_id, process_status, creator_id) VALUES (10, 1, ?, 'avatar/10.png', 'avatar.png', 'png', 10, 'avatar', 1, ?, 1)", StorageType.LOCAL.getCode(), FileProcessStatus.Activated.getCode());
        jdbcTemplate.update("UPDATE `user` SET avatar_file_id = 10 WHERE id = 1");
        associate(1L, 1L, 1L);
        associate(2L, 1L, 2L);

        var response = authRequest(token).queryParam("pageSize", 12).get(MEMBERS);
        response.then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(2))
                .body("data.list", hasSize(2))
                .body("data.list[0].userId", equalTo(1))
                .body("data.list[0].nickname", equalTo("昵称"))
                .body("data.list[0].avatarUrl", containsString("avatar/10.png"))
                .body("data.list[0].isCreator", equalTo(true))
                .body("data.list[1].nickname", equalTo("member2"))
                .body("data.list[1].isCreator", equalTo(false));
        assertThat(response.jsonPath().getMap("data.list[0]").keySet())
                .containsExactlyInAnyOrder("userId", "nickname", "avatarUrl", "isCreator", "completedCount");
        response.then()
                .body("data.list[0].completedCount", equalTo(0))
                .body("data.list[0].lastSubmittedAt", nullValue())
                .body("data.list[1].completedCount", equalTo(0))
                .body("data.list[1].lastSubmittedAt", nullValue());
    }

    /** 工坊退出立即消失，重新加入及重复加入均只显示一次。 */
    @Test
    void shouldExcludeExitedAndDeduplicateRejoinedMembers() {
        String token = prepareProject();
        addMember(2L);
        associate(1L, 1L, 2L);
        String memberToken = loginAs("member2", "123456");
        authRequest(memberToken, 1L).delete("/app/projects/1/workshop").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).get(MEMBERS).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(0)).body("data.list", hasSize(0));
        for (int i = 0; i < 2; i++) {
            authRequest(memberToken, 1L).post("/app/projects/1/workshop").then()
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        }
        authRequest(token).get(MEMBERS).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1)).body("data.list", hasSize(1))
                .body("data.list[0].userId", equalTo(2));
    }

    /** 已删除组织成员/用户、其他企划及其他组织的成员不得混入。 */
    @Test
    void shouldExcludeDeletedAndUnrelatedMembers() {
        String token = prepareProject();
        for (long id = 2; id <= 5; id++) {
            addMember(id);
        }
        jdbcTemplate.update("UPDATE organization_member SET deleted_at = NOW() WHERE user_id = 2");
        jdbcTemplate.update("UPDATE `user` SET deleted_at = NOW() WHERE id = 3");
        jdbcTemplate.update("UPDATE organization_member SET org_id = 2 WHERE user_id = 4");
        associate(2L, 1L, 2L);
        associate(3L, 1L, 3L);
        associate(4L, 1L, 4L);
        associate(5L, 2L, 5L);
        authRequest(token).get(MEMBERS).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(0)).body("data.list", hasSize(0));
    }

    /** 分页按稳定用户 ID 排序，越界页仍返回完整总数。 */
    @Test
    void shouldPaginateMembersAndKeepTotalBeyondLastPage() {
        String token = prepareProject();
        for (long id = 2; id <= 14; id++) {
            addMember(id);
            associate(id, 1L, id);
        }
        authRequest(token).queryParam("pageSize", 12).get(MEMBERS).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(13)).body("data.list", hasSize(12))
                .body("data.list[0].userId", equalTo(2));
        authRequest(token).queryParam("pageSize", 12).queryParam("pageNum", 2).get(MEMBERS).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(13)).body("data.list", hasSize(1))
                .body("data.list[0].userId", equalTo(14));
        authRequest(token).queryParam("pageSize", 12).queryParam("pageNum", Integer.MAX_VALUE).get(MEMBERS).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(13)).body("data.list", hasSize(0));
    }

    /** 无关联时不凭负责人字段制造成员。 */
    @Test
    void shouldReturnEmptyMembers() {
        String token = prepareProject();
        authRequest(token).get(MEMBERS).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(0)).body("data.list", hasSize(0));
    }

    /** 未认证请求不能获取企划成员。 */
    @Test
    void shouldRejectWithoutAuthentication() {
        request().get(MEMBERS).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /** 复用通用分页参数校验。 */
    @Test
    void shouldRejectInvalidPagination() {
        String token = prepareProject();
        for (String query : new String[]{"pageNum=0", "pageNum=-1", "pageSize=0", "pageSize=-1", "pageSize=101", "pageNum=abc"}) {
            authRequest(token).get(MEMBERS + "?" + query).then().statusCode(200)
                    .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        }
    }

    /** 跨组织、禁用、已删除或不存在的企划统一不可见。 */
    @Test
    void shouldRejectInvisibleProject() {
        String token = prepareProject();
        createOrganization(2L, "其他组织");
        createOrganizationMember(2L, 2L, 1L, "ORG_USER");
        authRequest(token, 2L).get(MEMBERS).then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        authRequest(token).get("/app/projects/999/members").then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        jdbcTemplate.update("UPDATE project SET resource_status = 0 WHERE id = 1");
        authRequest(token).get(MEMBERS).then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        jdbcTemplate.update("UPDATE project SET resource_status = 1, deleted_at = NOW() WHERE id = 1");
        authRequest(token).get(MEMBERS).then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /** 完成数按当前有效提交的任务计一次；打回、重置、软删和其他企划都不计入，时间取剩余有效提交的最晚值。 */
    @Test
    void shouldCountOnlyCurrentValidSubmissions() {
        String token = prepareProject();
        addMember(2L);
        addMember(3L);
        associate(1L, 1L, 1L);
        associate(2L, 1L, 2L);
        associate(3L, 1L, 3L);
        submission(1L, 1L, 10L, 1L, 1, TaskSubmissionStatus.REJECTED, "2026-03-04 11:00:00");
        submission(2L, 1L, 10L, 1L, 2, TaskSubmissionStatus.SUBMITTED, "2026-03-02 09:00:00");
        submission(3L, 1L, 11L, 1L, 1, TaskSubmissionStatus.SUBMITTED, "2026-03-03 10:00:00");
        submission(4L, 1L, 12L, 1L, 1, TaskSubmissionStatus.RESET, "2026-03-06 13:00:00");
        submission(5L, 1L, 13L, 1L, 1, TaskSubmissionStatus.SUBMITTED, "2026-03-07 14:00:00");
        jdbcTemplate.update("UPDATE task_submission SET deleted_at = NOW() WHERE id = 5");
        submission(6L, 1L, 14L, 1L, 1, TaskSubmissionStatus.REJECTED, "2026-03-01 08:00:00");
        submission(7L, 1L, 14L, 2L, 2, TaskSubmissionStatus.SUBMITTED, "2026-03-05 12:00:00");
        submission(8L, 2L, 15L, 1L, 1, TaskSubmissionStatus.SUBMITTED, "2026-03-08 15:00:00");

        authRequest(token).queryParam("pageSize", 12).get(MEMBERS).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[0].userId", equalTo(1))
                .body("data.list[0].completedCount", equalTo(2))
                .body("data.list[0].lastSubmittedAt", equalTo("2026-03-03 10:00:00"))
                .body("data.list[1].userId", equalTo(2))
                .body("data.list[1].completedCount", equalTo(1))
                .body("data.list[1].lastSubmittedAt", equalTo("2026-03-05 12:00:00"))
                .body("data.list[2].userId", equalTo(3))
                .body("data.list[2].completedCount", equalTo(0))
                .body("data.list[2].lastSubmittedAt", nullValue());
    }

    private String prepareProject() {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_USER");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, creator_id) VALUES (1, 1, '类型', 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (1, 1, 1, '企划', 1)");
        return loginAs("creator", "123456");
    }

    private void addMember(long id) {
        createTestUser(id, "member" + id, "123456", "member" + id + "@example.com");
        createOrganizationMember(id, 1L, id, "ORG_USER");
    }

    private void associate(long id, long projectId, long userId) {
        jdbcTemplate.update("INSERT INTO project_workshop (id, project_id, user_id) VALUES (?, ?, ?)", id, projectId, userId);
    }

    private void submission(long id, long projectId, long taskId, long submitterId, int version,
                            TaskSubmissionStatus status, String createdAt) {
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, status, created_at) "
                        + "VALUES (?, ?, 1, ?, 1, ?, ?, ?, ?)",
                id, projectId, taskId, submitterId, version, status.getCode(), createdAt);
    }
}
