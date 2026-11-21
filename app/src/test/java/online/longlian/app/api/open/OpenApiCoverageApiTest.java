package online.longlian.app.api.open;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.OpenResultCode;
import online.longlian.app.common.util.OpenCursorCodec;
import online.longlian.app.common.util.OpenIdempotencySupport;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetParamsBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskRowBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceDetailResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskSubmissionVersionResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskSubmissionVersionsParamsBO;
import online.longlian.app.pojo.bo.open.IdempotencyCheckParamsBO;
import online.longlian.app.pojo.bo.open.OpenCursorBO;
import online.longlian.app.pojo.bo.open.OpenSubmissionVersionBO;
import online.longlian.app.pojo.bo.open.OpenTaskDetailBO;
import online.longlian.app.pojo.bo.open.OpenTaskDetailParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmissionBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmissionsParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.common.exception.OpenAppException;
import online.longlian.app.common.properties.OpenApiProperties;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.controller.open.OpenTaskReadController;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.service.app.mytask.impl.MyTaskServiceImpl;
import online.longlian.app.service.app.taskinstance.TaskInstanceService;
import online.longlian.app.service.open.OpenApiKeyServiceImpl;
import online.longlian.app.service.open.OpenProjectServiceImpl;
import online.longlian.app.service.open.OpenTaskCommandServiceImpl;
import online.longlian.app.service.open.OpenTaskQueryServiceImpl;
import online.longlian.app.service.open.OpenApiRateLimitService;
import online.longlian.app.service.open.OpenTaskQueryService;
import online.longlian.common.enumeration.IdempotencySource;
import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import online.longlian.app.common.filter.OpenApiCachedBodyRequestWrapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 开放面守护分支的覆盖测试：限流窗口与降级、幂等上下文校验、站内码映射、
 * 任务命令与查询的空态守卫、清理任务手动触发。本类用 @DynamicPropertySource
 * 把限流压到 1 次/分钟，Spring 会为它单独建一个上下文缓存键，不影响其他测试类。
 */
public class OpenApiCoverageApiTest extends BaseApiTest {

    private static final long USER_ID = 26001L;
    private static final long ORG_ID = 27000L;
    private static final long MEMBER_ID = 28000L;

    @DynamicPropertySource
    static void oneRequestPerMinute(DynamicPropertyRegistry registry) {
        registry.add("longlian.open-api.rate-limit-per-minute", () -> "1");
    }

    private String createKey() {
        return createKey(USER_ID, ORG_ID, MEMBER_ID, "coveruser");
    }

    /** 每把密钥独立限流桶；同一用例内多次调用开放面时用独立用户+组织各建一把。 */
    private String createKey(long userId, long orgId, long memberId, String username) {
        createUserWithOrganization(userId, username, "123456", username + "@example.com", memberId, orgId, "ORG_USER");
        useRequestOrg(orgId);
        String token = loginAs(username, "123456");
        Response response = authRequest(token)
                .body(Map.of("name", "覆盖"))
                .post("/app/openapi-keys");
        return response.jsonPath().getString("data.key");
    }

    @Test
    void shouldReturnRateLimitedOnSecondRequestInWindow() {
        String key = createKey();

        given().header("X-Api-Key", key).get("/open/v1/projects").then().statusCode(200);
        given().header("X-Api-Key", key).get("/open/v1/projects").then()
                .statusCode(429)
                .body("code", equalTo(10005))
                .body("error", equalTo("OPEN_RATE_LIMITED"))
                .body("requestId", notNullValue());
    }

    @Test
    void shouldRejectCrossEndpointCursorOnMyTasksAndPlaza() {
        String key = createKey();
        String idOnlyCursor = OpenCursorCodec.encode(new OpenCursorBO(1L, null));

        given().header("X-Api-Key", key)
                .queryParam("cursor", idOnlyCursor)
                .get("/open/v1/my-tasks").then()
                .statusCode(400)
                .body("code", equalTo(10002));
        given().header("X-Api-Key", createKey(USER_ID + 1, ORG_ID + 1, MEMBER_ID + 1, "coveruser2"))
                .queryParam("cursor", idOnlyCursor)
                .get("/open/v1/task-plaza").then()
                .statusCode(400)
                .body("code", equalTo(10002));
    }

    @Test
    void shouldReturnOpenParamErrorForMalformedBody() {
        String key = createKey();

        given().header("X-Api-Key", key).header("X-Idempotency-Key", "cover-malformed")
                .contentType("application/json").body("{bad json")
                .post("/open/v1/files").then()
                .statusCode(400)
                .body("code", equalTo(10002))
                .body("error", equalTo("OPEN_PARAM_ERROR"));
    }

    @Test
    void shouldReturnStationNotFoundEnvelopeForUnknownStationPath() {
        createUserWithOrganization(USER_ID, "coveruser", "123456", "cover@example.com", MEMBER_ID, ORG_ID, "ORG_USER");
        String token = loginAs("coveruser", "123456");

        given().header("Authorization", "Bearer " + token)
                .header("X-Org-Id", Long.toString(ORG_ID))
                .get("/app/not-exist").then()
                .statusCode(200)
                .body("code", equalTo(4));
    }

    @Test
    void shouldTriggerIdempotencyRecordCleanupTask() {
        String token = createRootAdmin();

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json").body("{}")
                .post("/admin/scheduled-tasks/idempotency-record-cleanup/trigger").then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    @Test
    void shouldCoverProjectsUpdatedSinceFilter() {
        String key = createKey();
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, status, resource_status, creator_id, created_at, updated_at) "
                        + "VALUES (56001, ?, 0, '增量企划', 1, 1, ?, NOW(), NOW())",
                ORG_ID, USER_ID);

        given().header("X-Api-Key", key)
                .queryParam("updatedSince", "2020-01-01 00:00:00")
                .get("/open/v1/projects").then()
                .statusCode(200)
                .body("data.list.size()", greaterThan(0));
    }

    @Test
    void shouldReturnNotFoundForClaimOfMissingInstance() {
        String key = createKey();

        given().header("X-Api-Key", key).header("X-Idempotency-Key", "cover-missing")
                .post("/open/v1/tasks/999999/claim").then()
                .statusCode(404)
                .body("code", equalTo(10004))
                .body("error", equalTo("OPEN_NOT_FOUND"));
    }

    @Test
    void shouldDegradeRateLimitWhenRedisUnavailable() {
        RedissonClient broken = mock(RedissonClient.class);
        when(broken.getRateLimiter(anyString())).thenThrow(new RuntimeException("redis down"));

        OpenApiRateLimitService service = new OpenApiRateLimitService(broken, new OpenApiProperties());

        assertThat(service.tryAcquire(1L)).isTrue();
    }

    @Test
    void shouldMapUncoveredStationCodesToOpenSemantics() {
        assertThat(OpenResultCode.of(3)).isEqualTo(OpenResultCode.FORBIDDEN);
        assertThat(OpenResultCode.of(999)).isEqualTo(OpenResultCode.INTERNAL);
    }

    @Test
    void shouldRejectMissingAndOversizedIdempotencyKey() {
        MockHttpServletRequest missing = new MockHttpServletRequest("POST", "/open/v1/tasks/1/claim");
        assertThatThrownBy(() -> OpenIdempotencySupport.buildParams(missing, IdempotencySource.OPEN_API, 1L))
                .isInstanceOf(OpenAppException.class)
                .hasMessageContaining("必须携带");

        MockHttpServletRequest oversized = new MockHttpServletRequest("POST", "/open/v1/tasks/1/claim");
        oversized.addHeader("X-Idempotency-Key", "x".repeat(65));
        assertThatThrownBy(() -> OpenIdempotencySupport.buildParams(oversized, IdempotencySource.OPEN_API, 1L))
                .isInstanceOf(OpenAppException.class)
                .hasMessageContaining("过长");

        MockHttpServletRequest noBodyHash = new MockHttpServletRequest("POST", "/open/v1/tasks/1/claim");
        noBodyHash.addHeader("X-Idempotency-Key", "valid-length-key");
        assertThatThrownBy(() -> OpenIdempotencySupport.buildParams(noBodyHash, IdempotencySource.OPEN_API, 1L))
                .isInstanceOf(OpenAppException.class)
                .hasMessageContaining("幂等上下文缺失");
    }

    @Test
    void shouldCoverTaskDetailAndVersionNullGuards() {
        TaskInstanceService taskInstanceService = mock(TaskInstanceService.class);
        TaskInstanceMapper taskInstanceMapper = mock(TaskInstanceMapper.class);
        TaskSubmissionMapper taskSubmissionMapper = mock(TaskSubmissionMapper.class);
        ItemTaskNodeMapper itemTaskNodeMapper = mock(ItemTaskNodeMapper.class);
        ItemMapper itemMapper = mock(ItemMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        OpenTaskQueryServiceImpl service = new OpenTaskQueryServiceImpl(
                taskInstanceService, taskInstanceMapper, taskSubmissionMapper, itemTaskNodeMapper, itemMapper, projectMapper);

        TaskInstance instance = TaskInstance.builder()
                .id(1L).projectId(2L).itemId(3L).itemTaskNodeId(4L).revision(5L)
                .build();
        when(taskInstanceMapper.selectById(1L)).thenReturn(instance);
        when(itemTaskNodeMapper.selectById(4L)).thenReturn(mock(ItemTaskNode.class));
        when(itemMapper.selectById(3L)).thenReturn(mock(Item.class));
        when(projectMapper.selectById(2L)).thenReturn(mock(Project.class));
        when(taskInstanceMapper.selectItemStageNodeStatuses(anyList())).thenReturn(List.of());
        when(taskSubmissionMapper.selectCount(any())).thenReturn(0L);

        // 详情：无接取人（assignee null）、未提交（fields null）
        TaskInstanceDetailResultBO unclaimed = new TaskInstanceDetailResultBO();
        unclaimed.setTask(new TaskInstanceDetailResultBO.TaskSummaryResultBO("1", "翻译", 1, "2", null));
        unclaimed.setSubmission(new TaskInstanceDetailResultBO.TaskSubmissionResultBO("not_submitted", null, null));
        when(taskInstanceService.getTaskInstanceDetail(any(TaskInstanceDetailParamsBO.class))).thenReturn(unclaimed);

        OpenTaskDetailBO detail = service.getTaskDetail(new OpenTaskDetailParamsBO(1L, ORG_ID));
        assertThat(detail.submission().values()).isEmpty();
        assertThat(detail.assignee()).isNull();

        // 详情：接取人 ID 为空串、文件字段附件缺失
        TaskInstanceDetailResultBO blankAssignee = new TaskInstanceDetailResultBO();
        blankAssignee.setTask(new TaskInstanceDetailResultBO.TaskSummaryResultBO(
                "1", "翻译", 1, "2", new TaskInstanceDetailResultBO.TaskAssigneeResultBO("", "无名", null)));
        online.longlian.app.pojo.bo.app.taskinstance.TaskAttachmentResultBO attachment =
                new online.longlian.app.pojo.bo.app.taskinstance.TaskAttachmentResultBO();
        attachment.setId("7");
        attachment.setName("译稿.txt");
        attachment.setSizeText("1KB");
        blankAssignee.setSubmission(new TaskInstanceDetailResultBO.TaskSubmissionResultBO("submitted", LocalDateTime.now(),
                List.of(new TaskInstanceDetailResultBO.TaskTextFieldResultBO("k", "标签", "text", "内容"),
                        new TaskInstanceDetailResultBO.TaskFileFieldResultBO("f", "附件", "file", attachment))));
        when(taskInstanceService.getTaskInstanceDetail(any(TaskInstanceDetailParamsBO.class))).thenReturn(blankAssignee);

        OpenTaskDetailBO withFile = service.getTaskDetail(new OpenTaskDetailParamsBO(1L, ORG_ID));
        assertThat(withFile.submission().values()).hasSize(2);

        // 版本列表：无提交人、提交人 ID 为空串
        when(taskInstanceService.listSubmissionVersions(any(TaskSubmissionVersionsParamsBO.class))).thenReturn(List.of(
                TaskSubmissionVersionResultBO.builder().versionNo(1).build(),
                TaskSubmissionVersionResultBO.builder().versionNo(2)
                        .submitter(new TaskSubmissionVersionResultBO.TaskParticipantResultBO("", "无名", null)).build(),
                TaskSubmissionVersionResultBO.builder().versionNo(3)
                        .submitter(new TaskSubmissionVersionResultBO.TaskParticipantResultBO("abc", "乱号", null)).build()));

        List<OpenSubmissionVersionBO> versions = service.listSubmissions(new OpenTaskSubmissionsParamsBO(1L, ORG_ID));
        assertThat(versions).hasSize(3);
    }

    @Test
    void shouldConvertDetailWithNullFieldsInController() {
        TaskInstanceService taskInstanceService = mock(TaskInstanceService.class);
        OpenTaskQueryService queryService = mock(OpenTaskQueryService.class);
        OpenTaskReadController controller = new OpenTaskReadController(taskInstanceService, queryService);
        when(queryService.getTaskDetail(any(OpenTaskDetailParamsBO.class))).thenReturn(new OpenTaskDetailBO(
                1L, "翻译", 1, 1, 1, null, 3L, "项目", 2L, "企划", null, 1L, null,
                new OpenTaskSubmissionBO("not_submitted", null, null), 0L));

        var vo = controller.getTaskDetail(new OpenApiPrincipal(9L, USER_ID, ORG_ID), 1L);

        assertThat(vo).isNotNull();
    }

    @Test
    void shouldReturnEmptyMyTasksPageBeyondCursorEnd() {
        TaskInstanceMapper taskInstanceMapper = mock(TaskInstanceMapper.class);
        MyTaskServiceImpl service = new MyTaskServiceImpl(taskInstanceMapper);

        MyTaskRowBO row = new MyTaskRowBO();
        row.setInstanceId(1L);
        row.setItemId(3L);
        row.setProjectId(2L);
        row.setItemTitle("项目");
        row.setProjectName("企划");
        row.setNodeName("翻译");
        row.setNodeId(4L);
        row.setStageSort(1);
        row.setClaimedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        row.setRevision(5L);
        when(taskInstanceMapper.selectMyClaimedTasks(anyLong(), anyLong(), anyBoolean())).thenReturn(List.of(row));
        when(taskInstanceMapper.selectItemStageNodeStatuses(anyList())).thenReturn(List.of());

        var page = service.listMyTasksKeyset(new MyTaskKeysetParamsBO(
                USER_ID, ORG_ID, null, LocalDateTime.of(2099, 1, 1, 0, 0), 999L, 20));

        assertThat(page.list()).isEmpty();
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void shouldReturnEmptyGroupsForUserWithoutActiveMembership() {
        var openApiKeyMapper = mock(online.longlian.app.mapper.OpenApiKeyMapper.class);
        var organizationMapper = mock(online.longlian.app.mapper.OrganizationMapper.class);
        var organizationMemberMapper = mock(online.longlian.app.mapper.OrganizationMemberMapper.class);
        var lockService = mock(online.longlian.app.service.lock.LockService.class);
        OpenApiProperties properties = new OpenApiProperties();
        OpenApiKeyServiceImpl service = new OpenApiKeyServiceImpl(
                openApiKeyMapper, organizationMapper, organizationMemberMapper, lockService, properties,
                Clock.systemDefaultZone());
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());

        assertThat(service.listGroups(999L)).isEmpty();
    }

    @Test
    void shouldCoverNullResourceInProjectReadUrls() {
        var projectMapper = mock(ProjectMapper.class);
        var resourceService = mock(online.longlian.app.service.resource.ResourceService.class);
        OpenProjectServiceImpl service = new OpenProjectServiceImpl(projectMapper, resourceService);

        Project project = Project.builder().id(56002L).orgId(ORG_ID).title("空封面").creatorId(USER_ID)
                .coverFileId(77L).build();
        when(projectMapper.selectList(any())).thenReturn(List.of(project));
        Map<Long, online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO> readUrls = new java.util.HashMap<>();
        readUrls.put(77L, null);
        readUrls.put(78L, new online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO(
                "https://example.test/78", ORG_ID, "cover/78"));
        when(resourceService.getResourceReadUrls(anyList())).thenReturn(readUrls);

        var page = service.listProjects(new online.longlian.app.pojo.bo.open.OpenProjectListParamsBO(
                USER_ID, ORG_ID, 20, null, null));

        assertThat(page.list()).hasSize(1);
    }

    @Test
    void shouldReturnNotFoundForClaimWhenItemMissingOrForeignOrg() {
        TaskInstanceService taskInstanceService = mock(TaskInstanceService.class);
        TaskInstanceMapper taskInstanceMapper = mock(TaskInstanceMapper.class);
        ItemMapper itemMapper = mock(ItemMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        TaskSubmissionMapper taskSubmissionMapper = mock(TaskSubmissionMapper.class);
        OpenTaskCommandServiceImpl service = new OpenTaskCommandServiceImpl(
                taskInstanceService, taskInstanceMapper, itemMapper, projectMapper, taskSubmissionMapper);

        TaskInstance instance = TaskInstance.builder().id(1L).itemId(3L).projectId(2L).revision(5L).build();
        when(taskInstanceMapper.selectById(1L)).thenReturn(instance);

        // 实例存在但项目条目缺失
        when(itemMapper.selectById(3L)).thenReturn(null);
        assertThatThrownBy(() -> service.claimTask(new online.longlian.app.pojo.bo.open.OpenTaskClaimParamsBO(
                1L, USER_ID, ORG_ID)))
                .isInstanceOf(OpenAppException.class)
                .hasMessageContaining("任务不存在");

        // 条目存在但组织不符，统一 NOT_FOUND 不泄露存在性
        Item existingItem = mock(Item.class);
        when(existingItem.getProjectId()).thenReturn(2L);
        when(itemMapper.selectById(3L)).thenReturn(existingItem);
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(ORG_ID + 1).build());
        assertThatThrownBy(() -> service.claimTask(new online.longlian.app.pojo.bo.open.OpenTaskClaimParamsBO(
                1L, USER_ID, ORG_ID)))
                .isInstanceOf(OpenAppException.class)
                .hasMessageContaining("任务不存在");

        // 提交后回查不到版本记录：整笔回滚语义下的内部一致性守卫
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(ORG_ID)
                .resourceStatus(online.longlian.common.enumeration.Status.ENABLED).build());
        when(taskSubmissionMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.submitTask(new online.longlian.app.pojo.bo.open.OpenTaskSubmitParamsBO(
                1L, USER_ID, ORG_ID, Map.of())))
                .isInstanceOf(OpenAppException.class)
                .hasMessageContaining("提交记录缺失");
    }

    @Test
    void shouldDriveCachedBodyWrapperStreamSurface() throws Exception {
        OpenApiCachedBodyRequestWrapper wrapper = new OpenApiCachedBodyRequestWrapper(
                new MockHttpServletRequest(), "abc".getBytes());
        assertThat(wrapper.getContentLength()).isEqualTo(3);
        assertThat(wrapper.getContentLengthLong()).isEqualTo(3L);

        var stream = wrapper.getInputStream();
        assertThat(stream.isReady()).isTrue();
        stream.setReadListener(null);
        assertThat(stream.isFinished()).isFalse();
        var text = new java.io.BufferedReader(new java.io.InputStreamReader(stream)).read();
        assertThat(text).isNotEqualTo(-1);
        while (stream.read() != -1) {
            // 读完整个缓存流
        }
        assertThat(stream.isFinished()).isTrue();
    }

    @Test
    void shouldReturnEmptyItemsAndMembersPageBeyondCursorEnd() {
        String key = createKey();
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, status, resource_status, creator_id, created_at, updated_at) "
                        + "VALUES (57001, ?, 0, '空项目企划', 1, 1, ?, NOW(), NOW())",
                ORG_ID, USER_ID);
        jdbcTemplate.update(
                "INSERT INTO `project_workshop` (id, project_id, user_id) VALUES (1, 57001, ?)", USER_ID);

        // 企划下没有任何项目
        given().header("X-Api-Key", key)
                .get("/open/v1/projects/57001/items").then()
                .statusCode(200)
                .body("data.list.size()", equalTo(0))
                .body("data.hasMore", equalTo(false));

        // 成员游标越过末尾，第二页为空；密钥直插同组织（createKey 每次新建组织，无法复用本组织）
        jdbcTemplate.update(
                "INSERT INTO `open_api_key` (id, org_id, user_id, name, key_hash, status, created_at, updated_at) "
                        + "VALUES (30001, ?, ?, 'jdbc-key', ?, 1, NOW(), NOW())",
                ORG_ID, USER_ID, "dd2b7ad7919197e24c0b39c81f6e4ac7c25500681da87e37a7b55889d60178dc");
        given().header("X-Api-Key", "oa_cover_jdbc_key")
                .queryParam("cursor", OpenCursorCodec.encode(new OpenCursorBO(USER_ID + 1000, null)))
                .get("/open/v1/projects/57001/members").then()
                .statusCode(200)
                .body("data.list.size()", equalTo(0))
                .body("data.hasMore", equalTo(false));
    }
}
