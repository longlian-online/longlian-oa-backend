package online.longlian.app.service.app.taskplaza.impl;

import online.longlian.app.common.enumeration.PlazaStageScope;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.taskinstance.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.taskplaza.PlazaProjectGroupResultBO;
import online.longlian.app.pojo.bo.app.taskplaza.PlazaProjectSummaryBO;
import online.longlian.app.pojo.bo.app.taskplaza.TaskPlazaKeysetParamsBO;
import online.longlian.app.pojo.bo.app.taskplaza.TaskPlazaKeysetResultBO;
import online.longlian.app.pojo.bo.app.taskplaza.TaskPlazaSearchParamsBO;
import online.longlian.app.service.app.item.ItemService;
import online.longlian.app.service.resource.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 任务广场键集分页单元测试：limit+1 判 hasMore、游标键回传、筛选参数转发到聚合 SQL。
 */
class TaskPlazaServiceImplKeysetTest {

    private static final Long ORG_ID = 8L;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 9, 0, 0);

    private TaskInstanceMapper taskInstanceMapper;
    private TaskPlazaAssembler taskPlazaAssembler;
    private TaskPlazaServiceImpl service;

    @BeforeEach
    void setUp() {
        taskInstanceMapper = mock(TaskInstanceMapper.class);
        taskPlazaAssembler = mock(TaskPlazaAssembler.class);
        ProjectTypeMapper projectTypeMapper = mock(ProjectTypeMapper.class);
        ResourceService resourceService = mock(ResourceService.class);
        ItemService itemService = mock(ItemService.class);
        service = new TaskPlazaServiceImpl(taskInstanceMapper, projectTypeMapper, resourceService,
                taskPlazaAssembler, itemService);
        when(taskInstanceMapper.selectItemStageNodeStatuses(anyList())).thenReturn(List.of());
        when(itemService.listItemStageSorts(anyList())).thenReturn(List.of());
        when(taskPlazaAssembler.assembleGroups(anyList(), anyList(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    List<PlazaProjectSummaryBO> summaries = invocation.getArgument(0);
                    return summaries.stream()
                            .map(summary -> PlazaProjectGroupResultBO.builder()
                                    .projectId(summary.getProjectId())
                                    .title(summary.getTitle())
                                    .build())
                            .toList();
                });
    }

    private PlazaProjectSummaryBO summary(long projectId, LocalDateTime updatedAt) {
        PlazaProjectSummaryBO summary = new PlazaProjectSummaryBO();
        summary.setProjectId(projectId);
        summary.setTitle("企划" + projectId);
        summary.setUpdatedAt(updatedAt);
        return summary;
    }

    @Test
    void shouldPageGroupsByProjectKeyWithHasMore() {
        when(taskInstanceMapper.selectPlazaProjectSummariesKeyset(any(), any(), any(), eq(3)))
                .thenReturn(List.of(summary(41001L, BASE), summary(41002L, BASE.plusHours(1)),
                        summary(41003L, BASE.plusHours(2))));

        TaskPlazaKeysetResultBO page = service.getTaskPlazaKeyset(
                new TaskPlazaKeysetParamsBO(ORG_ID, null, null, null, null, null, 2));

        assertThat(page.groups()).hasSize(2);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.lastProjectId()).isEqualTo(41002L);
        assertThat(page.lastProjectUpdatedAt()).isEqualTo(BASE.plusHours(1));
    }

    @Test
    void shouldPassCursorAndFiltersIntoAggregateQuery() {
        when(taskInstanceMapper.selectPlazaProjectSummariesKeyset(any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        service.getTaskPlazaKeyset(new TaskPlazaKeysetParamsBO(
                ORG_ID, 43901L, "关键词", PlazaStageScope.ALL, BASE, 41001L, 50));

        ArgumentCaptor<TaskPlazaSearchParamsBO> searchCaptor =
                ArgumentCaptor.forClass(TaskPlazaSearchParamsBO.class);
        verify(taskInstanceMapper).selectPlazaProjectSummariesKeyset(
                searchCaptor.capture(), eq(BASE), eq(41001L), eq(51));
        TaskPlazaSearchParamsBO search = searchCaptor.getValue();
        assertThat(search.getOrgId()).isEqualTo(ORG_ID);
        assertThat(search.getBaseTaskId()).isEqualTo(43901L);
        assertThat(search.getMatchPattern()).isEqualTo("%关键词%");
        assertThat(search.getStageScope()).isEqualTo(PlazaStageScope.ALL);
    }

    @Test
    void shouldReturnEmptyPageWithoutCursorWhenNoGroups() {
        when(taskInstanceMapper.selectPlazaProjectSummariesKeyset(any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        TaskPlazaKeysetResultBO page = service.getTaskPlazaKeyset(
                new TaskPlazaKeysetParamsBO(ORG_ID, null, null, null, null, null, 50));

        assertThat(page.groups()).isEmpty();
        assertThat(page.hasMore()).isFalse();
        assertThat(page.lastProjectId()).isNull();
        assertThat(page.lastProjectUpdatedAt()).isNull();
    }

    @Test
    void shouldReturnFullPageWithoutHasMoreAtTail() {
        when(taskInstanceMapper.selectPlazaProjectSummariesKeyset(any(), any(), any(), eq(3)))
                .thenReturn(List.of(summary(41001L, BASE), summary(41002L, BASE.plusHours(1))));

        TaskPlazaKeysetResultBO page = service.getTaskPlazaKeyset(
                new TaskPlazaKeysetParamsBO(ORG_ID, null, null, null, null, null, 2));

        assertThat(page.groups()).hasSize(2);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.lastProjectId()).isEqualTo(41002L);
    }
}
