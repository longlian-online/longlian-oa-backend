package online.longlian.app.service.app.impl.itemtaskflow;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.ItemTaskFlowUpdateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAccessResultBO;
import online.longlian.app.pojo.bo.common.ResourceAbandonParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskFlow;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.orgadmin.BaseTaskService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import online.longlian.app.service.app.TaskDraftRecordService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemTaskFlowEditorTest {
    @Mock private ProjectService projectService;
    @Mock private BaseTaskService baseTaskService;
    @Mock private ItemMapper itemMapper;
    @Mock private ItemTaskFlowMapper flowMapper;
    @Mock private ItemTaskNodeMapper nodeMapper;
    @Mock private TaskInstanceMapper instanceMapper;
    @Mock private TaskSubmissionMapper submissionMapper;
    @Mock private BaseTaskMapper baseTaskMapper;
    @Mock private ResourceService resourceService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private ItemTaskFlowEditor editor;

    @Mock private TaskDraftRecordService taskDraftRecordService;

    @BeforeEach
    void setUp() {
        for (Class<?> entity : List.of(ItemTaskFlow.class, ItemTaskNode.class, TaskInstance.class, TaskSubmission.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entity);
        }
        editor = new ItemTaskFlowEditor(projectService, baseTaskService, itemMapper, flowMapper,
                nodeMapper, instanceMapper, submissionMapper, baseTaskMapper, resourceService, clock,
                taskDraftRecordService);
    }

    @Test
    void rejectsMissingItemBeforeCheckingProjectAccess() {
        expectFailure(params(List.of(), null), ResultCode.DATA_NOT_EXIT, "项目不存在");
        verifyNoInteractions(projectService, flowMapper, nodeMapper, instanceMapper, resourceService);
    }

    @Test
    void rejectsNonCreatorWithoutPlanningManagerAccess() {
        accessibleProject(9L);
        expectFailure(params(List.of(), null), ResultCode.UNAUTHORIZED_OPERATION, "仅企划负责人可编辑项目工作流");
        verify(projectService).isPlanningManager(9L, 3L, 2L);
        verifyNoInteractions(flowMapper, nodeMapper, instanceMapper);
    }

    @Test
    void rejectsMissingFlow() {
        accessibleProject(3L);
        expectFailure(params(List.of(), null), ResultCode.DATA_NOT_EXIT, "项目任务流不存在");
        verifyNoInteractions(nodeMapper, instanceMapper);
    }

    @Test
    void rejectsDuplicateInstancesForOneNode() {
        lockedFlow(List.of(currentNode()), List.of(instance(0L), instance(0L)));
        expectFailure(params(List.of(desired(10L, 0L)), null), ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.STALE);
        verifyNoInteractions(submissionMapper, baseTaskService, resourceService);
        verify(instanceMapper, never()).update(isNull(), any(Wrapper.class));
    }

    @Test
    void rejectsNodeWithoutInstance() {
        lockedFlow(List.of(currentNode()), List.of());
        expectFailure(params(List.of(desired(10L, 0L)), null), ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.STALE);
        verifyNoInteractions(submissionMapper, baseTaskService, resourceService);
    }

    @Test
    void rejectsInstanceWithoutRevision() {
        lockedFlow(List.of(currentNode()), List.of(instance(null)));
        expectFailure(params(List.of(desired(10L, 0L)), null), ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.STALE);
        verify(instanceMapper, never()).update(isNull(), any(Wrapper.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"baseTask", "sort", "parallelSort"})
    void rejectsEachMissingPositionField(String field) {
        lockedFlow(List.of(), List.of());
        ItemTaskFlowUpdateParamsBO.Node node = ItemTaskFlowUpdateParamsBO.Node.builder()
                .baseTaskId(field.equals("baseTask") ? null : 20L)
                .sort(field.equals("sort") ? null : 1)
                .parallelSort(field.equals("parallelSort") ? null : 1).build();
        expectFailure(params(List.of(node), null), ResultCode.PARAM_ERROR, "流程位置无效");
        verifyNoInteractions(baseTaskService, baseTaskMapper, resourceService);
    }

    @Test
    void rejectsExistingDesiredNodeWithoutRevision() {
        lockedFlow(List.of(), List.of());
        expectFailure(params(List.of(desired(10L, null)), null), ResultCode.PARAM_ERROR, "任务修订号不能为空");
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "revision"})
    void rejectsEachMissingRemovalField(String field) {
        lockedFlow(List.of(), List.of());
        ItemTaskFlowUpdateParamsBO.Removed removed = ItemTaskFlowUpdateParamsBO.Removed.builder()
                .id(field.equals("id") ? null : 10L).revision(field.equals("revision") ? null : 0L).build();
        expectFailure(params(List.of(desired(null, null)), List.of(removed)), ResultCode.PARAM_ERROR, "任务修订号不能为空");
        verifyNoInteractions(baseTaskService, resourceService);
    }

    @Test
    void historicalSubmissionsPreventDeletingAnAbandonedPendingTask() {
        lockedFlow(List.of(currentNode()), List.of(instance(0L)));
        when(submissionMapper.selectList(any())).thenReturn(List.of(
                TaskSubmission.builder().taskInstanceId(30L).build(),
                TaskSubmission.builder().taskInstanceId(30L).build()));
        expectFailure(replaceParams(), ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.LOCKED_DELETE);
        verify(instanceMapper, never()).update(isNull(), any(Wrapper.class));
        verifyNoInteractions(baseTaskService, resourceService);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void rejectsKeptNodeWhenOptimisticUpdateDoesNotAffectExactlyOneRow(int updated) {
        lockedFlow(List.of(currentNode()), List.of(instance(0L)));
        when(instanceMapper.update(isNull(), any(Wrapper.class))).thenReturn(updated);
        expectFailure(params(List.of(desired(10L, 0L)), null), ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.STALE);
        verify(flowMapper, never()).update(isNull(), any(Wrapper.class));
        verifyNoInteractions(resourceService);
    }

    @Test
    void savesKeptNodeWithoutLoadingBaseTasksOrAbandoningResources() {
        lockedFlow(List.of(currentNode()), List.of(instance(0L)));
        when(instanceMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        editor.update(params(List.of(desired(10L, 0L)), null));
        verify(nodeMapper).update(isNull(), any(Wrapper.class));
        verify(flowMapper).update(isNull(), any(Wrapper.class));
        verifyNoInteractions(baseTaskService, baseTaskMapper, resourceService);
        verify(projectService, never()).isPlanningManager(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void rejectsDeletionWhenOptimisticUpdateDoesNotAffectExactlyOneRow(int updated) {
        lockedFlow(List.of(currentNode()), List.of(instance(0L)));
        when(baseTaskMapper.selectBatchIds(List.of(20L))).thenReturn(List.of(baseTask()));
        when(instanceMapper.update(isNull(), any(Wrapper.class))).thenReturn(updated);
        expectFailure(replaceParams(), ResultCode.OPERATION_FAIL, ItemTaskFlowEditPlanner.STALE);
        verify(nodeMapper, never()).update(isNull(), any(Wrapper.class));
        verify(nodeMapper, never()).insert(any(ItemTaskNode.class));
        verify(flowMapper, never()).update(isNull(), any(Wrapper.class));
        verifyNoInteractions(resourceService);
    }

    @Test
    void deletesNodeAndAbandonsItsTaskResourcesBeforeCreatingReplacement() {
        lockedFlow(List.of(currentNode()), List.of(instance(0L)));
        when(baseTaskMapper.selectBatchIds(List.of(20L))).thenReturn(List.of(baseTask()));
        when(instanceMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        editor.update(replaceParams());
        ArgumentCaptor<ResourceAbandonParamsBO> abandoned = ArgumentCaptor.forClass(ResourceAbandonParamsBO.class);
        verify(resourceService).abandonBound(abandoned.capture());
        assertThat(abandoned.getValue().getOrgId()).isEqualTo(2L);
        assertThat(abandoned.getValue().getBizType()).isEqualTo("task_submit");
        assertThat(abandoned.getValue().getBizIds()).containsExactly(30L);
        verify(nodeMapper).update(isNull(), any(Wrapper.class));
        verify(flowMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void rejectsBaseTaskMissingAfterLock() {
        lockedFlow(List.of(), List.of());
        when(baseTaskMapper.selectBatchIds(List.of(20L))).thenReturn(List.of());
        expectFailure(params(List.of(desired(null, null)), null), ResultCode.PARAM_ERROR, "任务不存在");
        verify(baseTaskService).lockBaseTasks(2L, List.of(20L));
        verify(nodeMapper, never()).insert(any(ItemTaskNode.class));
        verify(instanceMapper, never()).insert(any(TaskInstance.class));
        verify(flowMapper, never()).update(isNull(), any(Wrapper.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"自定义名称"})
    void planningManagerCanCreateNodeWithSnapshotAndDefaultOrCustomName(String name) {
        accessibleProject(9L);
        when(projectService.isPlanningManager(9L, 3L, 2L)).thenReturn(true);
        flowContents(List.of(), List.of());
        when(baseTaskMapper.selectBatchIds(List.of(20L))).thenReturn(List.of(baseTask()));
        when(nodeMapper.insert(any(ItemTaskNode.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ItemTaskNode.class).setId(40L);
            return 1;
        });
        editor.update(params(List.of(ItemTaskFlowUpdateParamsBO.Node.builder()
                .baseTaskId(20L).sort(5).parallelSort(3).name(name).build()), List.of()));
        ArgumentCaptor<ItemTaskNode> node = ArgumentCaptor.forClass(ItemTaskNode.class);
        verify(nodeMapper).insert(node.capture());
        assertThat(node.getValue().getName()).isEqualTo(name == null ? "原子任务" : name);
        assertThat(node.getValue().getMetaSchema()).isEqualTo("[]");
        assertThat(node.getValue().getSort()).isEqualTo(1);
        assertThat(node.getValue().getParallelSort()).isEqualTo(1);
        assertThat(node.getValue().getUpdatedAt()).isEqualTo(LocalDateTime.now(clock));
        ArgumentCaptor<TaskInstance> instance = ArgumentCaptor.forClass(TaskInstance.class);
        verify(instanceMapper).insert(instance.capture());
        assertThat(instance.getValue().getItemTaskNodeId()).isEqualTo(40L);
        assertThat(instance.getValue().getStatus()).isEqualTo(TaskInstanceStatus.PENDING);
        assertThat(instance.getValue().getProjectId()).isEqualTo(4L);
        assertThat(instance.getValue().getItemId()).isEqualTo(1L);
        assertThat(instance.getValue().getTaskFlowId()).isEqualTo(5L);
        verify(baseTaskService).lockBaseTasks(2L, List.of(20L));
        verify(flowMapper).update(isNull(), any(Wrapper.class));
        verifyNoInteractions(submissionMapper, resourceService);
    }

    private void accessibleProject(Long creatorId) {
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(4L).build());
        when(projectService.lockEnabledProject(any())).thenReturn(new ProjectAccessResultBO(creatorId));
    }

    private void lockedFlow(List<ItemTaskNode> nodes, List<TaskInstance> instances) {
        accessibleProject(3L);
        flowContents(nodes, instances);
    }

    private void flowContents(List<ItemTaskNode> nodes, List<TaskInstance> instances) {
        when(flowMapper.selectOne(any())).thenReturn(ItemTaskFlow.builder().id(5L).itemId(1L).build());
        when(nodeMapper.selectList(any())).thenReturn(nodes);
        when(instanceMapper.selectList(any())).thenReturn(instances);
    }

    private ItemTaskNode currentNode() {
        return ItemTaskNode.builder().id(10L).baseTaskId(20L).name("原名称").build();
    }

    private TaskInstance instance(Long revision) {
        return TaskInstance.builder().id(30L).itemTaskNodeId(10L).revision(revision)
                .status(TaskInstanceStatus.PENDING).build();
    }

    private BaseTask baseTask() {
        return BaseTask.builder().id(20L).name("原子任务").metaSchema("[]").build();
    }

    private ItemTaskFlowUpdateParamsBO.Node desired(Long id, Long revision) {
        return ItemTaskFlowUpdateParamsBO.Node.builder().id(id).baseTaskId(20L)
                .sort(1).parallelSort(1).revision(revision).build();
    }

    private ItemTaskFlowUpdateParamsBO replaceParams() {
        return params(List.of(desired(null, null)), List.of(
                ItemTaskFlowUpdateParamsBO.Removed.builder().id(10L).revision(0L).build()));
    }

    private ItemTaskFlowUpdateParamsBO params(List<ItemTaskFlowUpdateParamsBO.Node> nodes,
                                             List<ItemTaskFlowUpdateParamsBO.Removed> removed) {
        return ItemTaskFlowUpdateParamsBO.builder().itemId(1L).orgId(2L).operatorId(3L)
                .nodes(nodes).removed(removed).build();
    }

    private void expectFailure(ItemTaskFlowUpdateParamsBO params, ResultCode code, String message) {
        assertThatThrownBy(() -> editor.update(params)).isInstanceOfSatisfying(AppException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(code.getCode());
            assertThat(exception).hasMessage(message);
        });
    }
}
