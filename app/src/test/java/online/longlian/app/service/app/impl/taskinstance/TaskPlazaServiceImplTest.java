package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.common.enumeration.PlazaSortBy;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.PlazaItemStageBO;
import online.longlian.app.pojo.bo.app.PlazaProjectSummaryBO;
import online.longlian.app.pojo.bo.app.PlazaTaskRowBO;
import online.longlian.app.pojo.bo.app.TaskPlazaParamsBO;
import online.longlian.app.pojo.bo.app.TaskPlazaSearchParamsBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.vo.app.PlazaProjectGroupVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskPlazaServiceImplTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private ProjectTypeMapper projectTypeMapper;
    @Mock
    private ResourceService resourceService;
    @Mock
    private TaskPlazaAssembler taskPlazaAssembler;

    private TaskPlazaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskPlazaServiceImpl(taskInstanceMapper, projectTypeMapper,
                resourceService, taskPlazaAssembler);
    }

    @Test
    void getTaskPlaza_noProjects_returnsEmptyPageWithoutDetailQueries() {
        when(taskInstanceMapper.selectPlazaProjectCount(any())).thenReturn(0L);

        PageResultVO<PlazaProjectGroupVO> result = service.getTaskPlaza(params(PlazaSortBy.WAIT));

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verify(taskInstanceMapper, never()).selectPlazaProjectSummaries(any(), anyInt(), anyInt());
        verify(taskInstanceMapper, never()).selectPlazaTaskRows(any(), anyList());
        verify(taskInstanceMapper, never()).selectPlazaItemStages(anyList());
    }

    @Test
    void getTaskPlaza_buildsSearchParamsAndAssembles() {
        when(taskInstanceMapper.selectPlazaProjectCount(any())).thenReturn(1L);
        PlazaProjectSummaryBO summary = new PlazaProjectSummaryBO();
        summary.setProjectId(100L);
        summary.setCoverFileId(7L);
        when(taskInstanceMapper.selectPlazaProjectSummaries(any(), anyInt(), anyInt()))
                .thenReturn(List.of(summary));
        when(taskInstanceMapper.selectPlazaTaskRows(any(), anyList()))
                .thenReturn(List.of(row(11L, 100L, 200L, 1)));
        when(taskInstanceMapper.selectPlazaItemStages(anyList()))
                .thenReturn(List.of(stage(200L, 2), stage(200L, 1), stage(200L, 2)));
        when(resourceService.getResourceReadUrls(anyList()))
                .thenReturn(Map.of(7L, new ResourceReadUrlGetResultBO("http://cover/7", 10L, "key-7")));
        PlazaProjectGroupVO group = PlazaProjectGroupVO.builder().projectId(100L).build();
        when(taskPlazaAssembler.assembleGroups(anyList(), anyList(), anyMap(), anyMap()))
                .thenReturn(List.of(group));

        PageResultVO<PlazaProjectGroupVO> result = service.getTaskPlaza(params(PlazaSortBy.WAIT));

        assertThat(result.getList()).containsExactly(group);
        assertThat(result.getTotal()).isEqualTo(1L);

        ArgumentCaptor<TaskPlazaSearchParamsBO> paramsCaptor =
                ArgumentCaptor.forClass(TaskPlazaSearchParamsBO.class);
        verify(taskInstanceMapper).selectPlazaProjectSummaries(paramsCaptor.capture(), anyInt(), anyInt());
        assertThat(paramsCaptor.getValue().getOrgId()).isEqualTo(10L);
        assertThat(paramsCaptor.getValue().getTypeId()).isNull();
        assertThat(paramsCaptor.getValue().getMatchPattern()).isNull();
        assertThat(paramsCaptor.getValue().getSortBy()).isEqualTo(PlazaSortBy.WAIT);

        verify(taskInstanceMapper).selectPlazaProjectSummaries(any(), eq(8), eq(8)); // 第2页 offset=8
        verify(taskInstanceMapper).selectPlazaTaskRows(any(), eq(List.of(100L)));
        verify(taskInstanceMapper).selectPlazaItemStages(List.of(200L));
        // 阶段 sort 去重升序后传入装配器
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, List<Integer>>> sortsCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(taskPlazaAssembler).assembleGroups(anyList(), anyList(), sortsCaptor.capture(), anyMap());
        assertThat(sortsCaptor.getValue()).containsEntry(200L, List.of(1, 2));
        verify(taskPlazaAssembler).assembleGroups(anyList(), anyList(), anyMap(),
                eq(Map.of(100L, "http://cover/7")));
    }

    @Test
    void getTaskPlaza_keywordEscaped_likeWildcardNeutralized() {
        when(taskInstanceMapper.selectPlazaProjectCount(any())).thenReturn(0L);

        TaskPlazaParamsBO params = TaskPlazaParamsBO.builder()
                .orgId(10L).keyword("100%_ good!").sortBy(PlazaSortBy.WAIT)
                .page(new PageParamsBO(1, 8))
                .build();
        service.getTaskPlaza(params);

        ArgumentCaptor<TaskPlazaSearchParamsBO> captor =
                ArgumentCaptor.forClass(TaskPlazaSearchParamsBO.class);
        verify(taskInstanceMapper).selectPlazaProjectCount(captor.capture());
        assertThat(captor.getValue().getMatchPattern()).isEqualTo("%100!%!_ good!!%");
    }

    @Test
    void getTaskPlaza_projectTypeResolvedToTypeId() {
        ProjectType type = new ProjectType();
        type.setId(77L);
        when(projectTypeMapper.selectOne(any())).thenReturn(type);
        when(taskInstanceMapper.selectPlazaProjectCount(any())).thenReturn(0L);

        TaskPlazaParamsBO params = TaskPlazaParamsBO.builder()
                .orgId(10L).projectType(" 漫画 ").sortBy(PlazaSortBy.MOST)
                .page(new PageParamsBO(1, 8))
                .build();
        service.getTaskPlaza(params);

        ArgumentCaptor<TaskPlazaSearchParamsBO> captor =
                ArgumentCaptor.forClass(TaskPlazaSearchParamsBO.class);
        verify(taskInstanceMapper).selectPlazaProjectCount(captor.capture());
        assertThat(captor.getValue().getTypeId()).isEqualTo(77L);
        assertThat(captor.getValue().getSortBy()).isEqualTo(PlazaSortBy.MOST);
    }

    @Test
    void getTaskPlaza_unknownProjectType_throws() {
        when(projectTypeMapper.selectOne(any())).thenReturn(null);

        TaskPlazaParamsBO params = TaskPlazaParamsBO.builder()
                .orgId(10L).projectType("不存在").sortBy(PlazaSortBy.WAIT)
                .page(new PageParamsBO(1, 8))
                .build();

        assertThatThrownBy(() -> service.getTaskPlaza(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("企划类型不存在或已禁用");
        verify(taskInstanceMapper, never()).selectPlazaProjectCount(any());
    }

    @Test
    void getTaskPlaza_coverFileIdFiltered_beforeBatchUrlLookup() {
        PlazaProjectSummaryBO noCover = new PlazaProjectSummaryBO();
        noCover.setProjectId(100L);
        noCover.setCoverFileId(0L);
        PlazaProjectSummaryBO withCover = new PlazaProjectSummaryBO();
        withCover.setProjectId(101L);
        withCover.setCoverFileId(7L);
        when(taskInstanceMapper.selectPlazaProjectCount(any())).thenReturn(2L);
        when(taskInstanceMapper.selectPlazaProjectSummaries(any(), anyInt(), anyInt()))
                .thenReturn(List.of(noCover, withCover));
        when(taskInstanceMapper.selectPlazaTaskRows(any(), anyList())).thenReturn(List.of());
        when(resourceService.getResourceReadUrls(anyList()))
                .thenReturn(Map.of(7L, new ResourceReadUrlGetResultBO("http://cover/7", 10L, "key-7")));
        when(taskPlazaAssembler.assembleGroups(anyList(), anyList(), anyMap(), anyMap()))
                .thenReturn(List.of());

        service.getTaskPlaza(params(PlazaSortBy.LATEST));

        verify(resourceService).getResourceReadUrls(List.of(7L));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, String>> coverCaptor = ArgumentCaptor.forClass(Map.class);
        verify(taskPlazaAssembler).assembleGroups(anyList(), anyList(), anyMap(), coverCaptor.capture());
        assertThat(coverCaptor.getValue()).containsOnlyKeys(101L);
    }

    private TaskPlazaParamsBO params(PlazaSortBy sortBy) {
        return TaskPlazaParamsBO.builder()
                .orgId(10L)
                .sortBy(sortBy)
                .page(new PageParamsBO(2, 8))
                .build();
    }

    private PlazaTaskRowBO row(Long instanceId, Long projectId, Long itemId, int sort) {
        PlazaTaskRowBO row = new PlazaTaskRowBO();
        row.setInstanceId(instanceId);
        row.setProjectId(projectId);
        row.setItemId(itemId);
        row.setSort(sort);
        row.setStatus(TaskInstanceStatus.PENDING);
        row.setCreatedAt(LocalDateTime.of(2026, 10, 1, 12, 0));
        return row;
    }

    private PlazaItemStageBO stage(Long itemId, int sort) {
        PlazaItemStageBO stage = new PlazaItemStageBO();
        stage.setItemId(itemId);
        stage.setSort(sort);
        return stage;
    }
}
