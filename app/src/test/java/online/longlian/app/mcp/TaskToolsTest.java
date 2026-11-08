package online.longlian.app.mcp;

import com.alibaba.fastjson2.JSON;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.app.common.enumeration.PlazaSortBy;
import online.longlian.app.common.enumeration.PlazaStageScope;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.app.MyTaskParamsBO;
import online.longlian.app.pojo.bo.app.TaskPlazaParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceAbandonParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.app.MyTaskVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.service.app.*;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskToolsTest {

    // 我的待办使用本人和本组织，默认查询可执行分组；所有 ID 保持十进制字符串。
    @Test
    void shouldListOwnTasksWithDefaultsAndStringIds() {
        McpUserSession session = mock(McpUserSession.class);
        MyTaskService myTasks = mock(MyTaskService.class);
        when(session.require(any())).thenReturn(new SessionContext(7L, 12L));
        when(myTasks.listMyTasks(any())).thenReturn(new PageResultVO<>(List.of(MyTaskVO.builder()
                .instanceId(9007199254740993L).itemId(2L).projectId(3L).build()), 1L));
        TaskTools tools = new TaskTools(session, null, null, null, null, null, null, myTasks, null, null);

        var result = JSON.parseObject(tools.listMyTasks(null, null, null, null, null));

        assertThat(result.getJSONArray("list").getJSONObject(0).get("instanceId")).isEqualTo("9007199254740993");
        assertThat(result.getJSONArray("list").getJSONObject(0).get("itemId")).isEqualTo("2");
        assertThat(result.getJSONArray("list").getJSONObject(0).get("projectId")).isEqualTo("3");
        ArgumentCaptor<MyTaskParamsBO> params = ArgumentCaptor.forClass(MyTaskParamsBO.class);
        verify(myTasks).listMyTasks(params.capture());
        assertThat(params.getValue().getUserId()).isEqualTo(7L);
        assertThat(params.getValue().getOrgId()).isEqualTo(12L);
        assertThat(params.getValue().getBucket()).isEqualTo(MyTaskBucket.SUBMITTABLE);
        assertThat(params.getValue().getSortDirection()).isEqualTo(SortDirection.ASC);
        assertThat(params.getValue().getPage().getPageSize()).isEqualTo(20);
    }

    // 任务广场的筛选、排序与分页直接映射给原服务，不改变其企划分组口径。
    @Test
    void shouldMapPlazaFiltersAndPagination() {
        McpUserSession session = mock(McpUserSession.class);
        TaskPlazaService plaza = mock(TaskPlazaService.class);
        when(session.require(any())).thenReturn(new SessionContext(7L, 12L));
        when(plaza.getTaskPlaza(any())).thenReturn(new PageResultVO<>(List.of(), 0L));
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            TaskTools tools = new TaskTools(session, null, null, null, null, null, null, null, plaza, factory.getValidator());
            tools.listClaimableTasks(null, "99", "翻译", "漫画", PlazaSortBy.MOST, PlazaStageScope.ALL, 2, 5);
            ArgumentCaptor<TaskPlazaParamsBO> params = ArgumentCaptor.forClass(TaskPlazaParamsBO.class);
            verify(plaza).getTaskPlaza(params.capture());
            assertThat(params.getValue().getOrgId()).isEqualTo(12L);
            assertThat(params.getValue().getBaseTaskId()).isEqualTo(99L);
            assertThat(params.getValue().getKeyword()).isEqualTo("翻译");
            assertThat(params.getValue().getProjectType()).isEqualTo("漫画");
            assertThat(params.getValue().getSortBy()).isEqualTo(PlazaSortBy.MOST);
            assertThat(params.getValue().getStageScope()).isEqualTo(PlazaStageScope.ALL);
            assertThat(params.getValue().getPage().getPageNum()).isEqualTo(2);
            assertThat(params.getValue().getPage().getPageSize()).isEqualTo(5);
        }
    }

    // 放弃保留用户读取的原修订号，由已有服务执行状态修改和暂存清理。
    @Test
    void shouldAbandonOwnTaskWithExpectedRevision() {
        McpUserSession session = mock(McpUserSession.class);
        TaskInstanceService tasks = mock(TaskInstanceService.class);
        when(session.require(any())).thenReturn(new SessionContext(7L, 12L));
        TaskInstanceDetailVO detail = new TaskInstanceDetailVO();
        detail.setTask(new TaskInstanceDetailVO.Task("9", "翻译", 1, "CLAIMED",
                new TaskInstanceDetailVO.Assignee("7", "本人", null)));
        when(tasks.getTaskInstanceDetail(any())).thenReturn(detail);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            TaskTools tools = new TaskTools(session, null, null, null, tasks, null, null, null, null, factory.getValidator());
            assertThat(JSON.parseObject(tools.abandonTask(null, "9", "4")).getBoolean("abandoned")).isTrue();
            ArgumentCaptor<TaskInstanceAbandonParamsBO> params = ArgumentCaptor.forClass(TaskInstanceAbandonParamsBO.class);
            verify(tasks).abandonTask(params.capture());
            assertThat(params.getValue().getInstanceId()).isEqualTo(9L);
            assertThat(params.getValue().getUserId()).isEqualTo(7L);
            assertThat(params.getValue().getOrgId()).isEqualTo(12L);
            assertThat(params.getValue().getExpectedRevision()).isEqualTo("4");
        }
    }

    // 上传固定任务业务类型，并只暴露直传需要的数据；长整型 ID 不变成 JSON 数字。
    @Test
    void shouldFixUploadBusinessAndPreserveSignedHeadersWithoutStorageKey() {
        McpUserSession session = mock(McpUserSession.class);
        ResourceService resources = mock(ResourceService.class);
        when(session.require(any())).thenReturn(new SessionContext(7L, 12L));
        when(resources.create(any())).thenReturn(new ResourceCreateVO(9007199254740993L, "https://upload.example/file",
                "private-storage-key", StorageType.COS, "attachment; filename=exact.txt"));
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            TaskTools tools = new TaskTools(session, null, null, null, null, null, resources, null, null, factory.getValidator());
            var result = JSON.parseObject(tools.createTaskUpload(null, "成果.txt", "txt", 3L, "text/plain"));

            assertThat(result.getString("fileId")).isEqualTo("9007199254740993");
            assertThat(result.getJSONObject("headers").getString("Content-Disposition")).isEqualTo("attachment; filename=exact.txt");
            assertThat(result).doesNotContainKeys("key", "storageType");
            ArgumentCaptor<ResourceCreateParamsBO> params = ArgumentCaptor.forClass(ResourceCreateParamsBO.class);
            verify(resources).create(params.capture());
            assertThat(params.getValue().getBizType()).isEqualTo("task_submit");
            assertThat(params.getValue().getCreatorId()).isEqualTo(7L);
            assertThat(params.getValue().getOrgId()).isEqualTo(12L);
        }
    }

    // 暂存只调用现有暂存服务，完整保留字段；不会额外发起正式提交。
    @Test
    void shouldSaveFullDraftAsCurrentUserWithoutSubmitting() {
        McpUserSession session = mock(McpUserSession.class);
        TaskInstanceService tasks = mock(TaskInstanceService.class);
        when(session.require(any())).thenReturn(new SessionContext(7L, 12L));
        when(tasks.saveTaskDraft(any())).thenReturn(9L);
        Map<String, Object> values = Map.of("text", "译文", "file", Map.of("fileId", "100"));
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            TaskTools tools = new TaskTools(session, null, null, null, tasks, null, null, null, null, factory.getValidator());
            assertThat(JSON.parseObject(tools.saveTaskDraft(null, "9", values)).getBoolean("saved")).isTrue();
            ArgumentCaptor<TaskInstanceSubmitParamsBO> params = ArgumentCaptor.forClass(TaskInstanceSubmitParamsBO.class);
            verify(tasks).saveTaskDraft(params.capture());
            assertThat(params.getValue().getUserId()).isEqualTo(7L);
            assertThat(params.getValue().getOrgId()).isEqualTo(12L);
            assertThat(params.getValue().getValues()).isEqualTo(values);
            verifyNoMoreInteractions(tasks);
        }
    }
}
