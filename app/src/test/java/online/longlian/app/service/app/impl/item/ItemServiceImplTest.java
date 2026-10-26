package online.longlian.app.service.app.impl.item;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.bo.app.ItemReorderParamsBO;
import online.longlian.app.pojo.bo.app.ItemSortUpdateBO;
import online.longlian.app.pojo.bo.app.ProjectAccessResultBO;
import online.longlian.app.pojo.bo.common.ResourceAbandonParamsBO;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskFlow;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskInstanceStatus;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock private ProjectMapper projectMapper;
    @Mock private ItemMapper itemMapper;
    @Mock private ItemTaskFlowMapper itemTaskFlowMapper;
    @Mock private ItemTaskNodeMapper itemTaskNodeMapper;
    @Mock private TaskTemplateMapper taskTemplateMapper;
    @Mock private TaskTemplateNodeMapper taskTemplateNodeMapper;
    @Mock private BaseTaskMapper baseTaskMapper;
    @Mock private TaskInstanceMapper taskInstanceMapper;
    @Mock private TaskSubmissionMapper taskSubmissionMapper;
    @Mock private ResourceService resourceService;
    @Mock private ItemQueryBuilder itemQueryBuilder;
    @Mock private ItemAssembler itemAssembler;
    @Mock private ProjectService projectService;

    private ItemServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskSubmission.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Item.class);
        service = new ItemServiceImpl(projectMapper, itemMapper, itemTaskFlowMapper, itemTaskNodeMapper,
                taskTemplateMapper, taskTemplateNodeMapper, baseTaskMapper, taskInstanceMapper,
                taskSubmissionMapper, resourceService, Clock.systemUTC(), itemQueryBuilder, itemAssembler, projectService);
        lenient().when(taskSubmissionMapper.selectCount(any())).thenReturn(0L);
        lenient().when(taskSubmissionMapper.selectObjs(any())).thenReturn(List.of());
        lenient().when(projectService.lockEnabledProject(any())).thenReturn(new ProjectAccessResultBO(1L));
    }

    @Test
    void listProjectItems_disabledProject_throwsNotFound() {
        when(projectMapper.selectById(1L)).thenReturn(disabledProject());
        ItemListParamsBO params = ItemListParamsBO.builder().projectId(1L).orgId(1L).build();

        assertThatThrownBy(() -> service.listProjectItems(params)).isInstanceOf(AppException.class);
        verify(itemMapper, never()).selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createProjectItem_disabledProject_throwsNotFound() {
        when(projectService.lockEnabledProject(any())).thenThrow(new AppException(ResultCode.DATA_NOT_EXIT));
        ItemCreateParamsBO params = ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build();

        assertThatThrownBy(() -> service.createProjectItem(params)).isInstanceOf(AppException.class);
        verify(taskTemplateMapper, never()).selectById(1L);
    }

    @Test
    void createProjectItem_createsEveryInstancePending() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskTemplate.class);
        when(taskTemplateMapper.selectById(1L)).thenReturn(
                TaskTemplate.builder().id(1L).name("模板").status(Status.ENABLED)
                        .scope(TaskTemplateScope.ORGANIZATION).orgId(1L).build());
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(
                TaskTemplateNode.builder().id(11L).baseTaskId(22L).sort(1).parallelSort(1).build(),
                TaskTemplateNode.builder().id(12L).baseTaskId(21L).sort(2).parallelSort(1).build(),
                TaskTemplateNode.builder().id(13L).baseTaskId(22L).sort(3).parallelSort(1).build()));
        when(baseTaskMapper.selectList(any())).thenReturn(List.of(
                BaseTask.builder().id(21L).orgId(1L).name("绘制").status(Status.ENABLED).metaSchema("[]").build(),
                BaseTask.builder().id(22L).orgId(1L).name("翻译").status(Status.ENABLED).metaSchema("[]").build()));

        service.createProjectItem(ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build());

        ArgumentCaptor<TaskInstance> captor = ArgumentCaptor.forClass(TaskInstance.class);
        verify(taskInstanceMapper, times(3)).insert(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(instance -> {
            assertThat(instance.getStatus()).isEqualTo(TaskInstanceStatus.PENDING);
            assertThat(instance.getAssigneeId()).isNull();
        });
        ArgumentCaptor<ItemTaskNode> nodes = ArgumentCaptor.forClass(ItemTaskNode.class);
        verify(itemTaskNodeMapper, times(3)).insert(nodes.capture());
        assertThat(nodes.getAllValues()).extracting(ItemTaskNode::getBaseTaskId).containsExactly(22L, 21L, 22L);
        assertThat(nodes.getAllValues()).extracting(ItemTaskNode::getName).containsExactly("翻译", "绘制", "翻译");
    }

    @Test
    void createProjectItem_lockedBaseTaskDisappears_rejectsWithoutPersisting() {
        when(taskTemplateMapper.selectById(1L)).thenReturn(
                TaskTemplate.builder().id(1L).name("模板").status(Status.ENABLED)
                        .scope(TaskTemplateScope.ORGANIZATION).orgId(1L).build());
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(
                TaskTemplateNode.builder().id(11L).baseTaskId(22L).sort(2).parallelSort(1).build(),
                TaskTemplateNode.builder().id(12L).baseTaskId(21L).sort(1).parallelSort(1).build()));
        when(baseTaskMapper.selectList(any()))
                .thenReturn(List.of(BaseTask.builder().id(21L).name("绘制").status(Status.ENABLED).metaSchema("[]").build()));

        assertThatThrownBy(() -> service.createProjectItem(ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build()))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(itemMapper, never()).insert(any(Item.class));
        verifyNoInteractions(itemTaskFlowMapper, itemTaskNodeMapper, taskInstanceMapper);
        verify(taskTemplateMapper, never()).update(isNull(), any());
    }

    @Test
    void createProjectItem_disabledBaseTask_copiesCurrentDefinitionAndCreatesPendingInstance() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskTemplate.class);
        when(taskTemplateMapper.selectById(1L)).thenReturn(
                TaskTemplate.builder().id(1L).name("模板").status(Status.ENABLED)
                        .scope(TaskTemplateScope.ORGANIZATION).orgId(1L).build());
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(
                TaskTemplateNode.builder().id(11L).baseTaskId(22L).sort(1).parallelSort(1).build(),
                TaskTemplateNode.builder().id(12L).baseTaskId(21L).sort(2).parallelSort(1).build()));
        when(baseTaskMapper.selectList(any())).thenReturn(List.of(
                BaseTask.builder().id(21L).orgId(1L).name("禁用后的最新名称").status(Status.DISABLED)
                        .metaSchema("[{\"key\":\"latest\"}]").build(),
                BaseTask.builder().id(22L).orgId(1L).name("启用任务").status(Status.ENABLED).metaSchema("[]").build()));

        service.createProjectItem(ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build());

        ArgumentCaptor<ItemTaskNode> nodes = ArgumentCaptor.forClass(ItemTaskNode.class);
        verify(itemTaskNodeMapper, times(2)).insert(nodes.capture());
        assertThat(nodes.getAllValues()).extracting(ItemTaskNode::getName)
                .containsExactly("启用任务", "禁用后的最新名称");
        assertThat(nodes.getAllValues()).extracting(ItemTaskNode::getMetaSchema)
                .containsExactly("[]", "[{\"key\":\"latest\"}]");
        ArgumentCaptor<TaskInstance> instances = ArgumentCaptor.forClass(TaskInstance.class);
        verify(taskInstanceMapper, times(2)).insert(instances.capture());
        assertThat(instances.getAllValues()).allSatisfy(instance ->
                assertThat(instance.getStatus()).isEqualTo(TaskInstanceStatus.PENDING));
    }


    @Test
    void createProjectItem_orgTemplateFromOtherOrg_rejects() {
        when(taskTemplateMapper.selectById(1L)).thenReturn(
                TaskTemplate.builder().id(1L).name("外组织模板").status(Status.ENABLED)
                        .scope(TaskTemplateScope.ORGANIZATION).orgId(99L).build());

        assertThatThrownBy(() -> service.createProjectItem(ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build()))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(itemMapper, never()).insert(any(Item.class));
        verify(taskTemplateMapper, never()).update(isNull(), any());
        verifyNoInteractions(itemTaskFlowMapper, itemTaskNodeMapper, taskInstanceMapper);
    }

    @Test
    void createProjectItem_othersPersonalTemplate_rejects() {
        when(taskTemplateMapper.selectById(1L)).thenReturn(
                TaskTemplate.builder().id(1L).name("他人个人模板").status(Status.ENABLED)
                        .scope(TaskTemplateScope.PERSONAL).orgId(1L).creatorId(99L).build());

        assertThatThrownBy(() -> service.createProjectItem(ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build()))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(itemMapper, never()).insert(any(Item.class));
        verify(taskTemplateMapper, never()).update(isNull(), any());
        verifyNoInteractions(itemTaskFlowMapper, itemTaskNodeMapper, taskInstanceMapper);
    }

    @Test
    void createProjectItem_unknownTemplateScope_rejects() {
        when(taskTemplateMapper.selectById(1L)).thenReturn(
                TaskTemplate.builder().id(1L).name("无作用域").status(Status.ENABLED).scope(null).build());

        assertThatThrownBy(() -> service.createProjectItem(ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("任务模板作用域无效");

        verify(itemMapper, never()).insert(any(Item.class));
        verifyNoInteractions(taskTemplateNodeMapper, itemTaskFlowMapper, itemTaskNodeMapper, taskInstanceMapper);
    }

    @Test
    void createProjectItem_ownPersonalTemplate_succeeds() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskTemplate.class);
        when(taskTemplateMapper.selectById(1L)).thenReturn(
                TaskTemplate.builder().id(1L).name("我的个人模板").status(Status.ENABLED)
                        .scope(TaskTemplateScope.PERSONAL).orgId(1L).creatorId(1L).build());
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(
                TaskTemplateNode.builder().id(11L).baseTaskId(22L).sort(1).parallelSort(1).build()));
        when(baseTaskMapper.selectList(any())).thenReturn(List.of(
                BaseTask.builder().id(22L).orgId(1L).name("启用任务").status(Status.ENABLED).metaSchema("[]").build()));
        when(itemMapper.selectOne(any())).thenReturn(Item.builder().sort(40L).build());

        service.createProjectItem(ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build());

        ArgumentCaptor<Item> created = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).insert(created.capture());
        assertThat(created.getValue().getSort()).isEqualTo(41L);
        verify(taskTemplateMapper).update(isNull(), any());
    }

    @Test
    void createProjectItem_baseTaskFromAnotherOrganization_rejectsWithoutCopyingName() {
        when(taskTemplateMapper.selectById(1L)).thenReturn(
                TaskTemplate.builder().id(1L).name("模板").status(Status.ENABLED)
                        .scope(TaskTemplateScope.ORGANIZATION).orgId(1L).build());
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(
                TaskTemplateNode.builder().id(11L).baseTaskId(22L).sort(1).parallelSort(1).build(),
                TaskTemplateNode.builder().id(12L).baseTaskId(21L).sort(2).parallelSort(1).build()));
        when(baseTaskMapper.selectList(any())).thenReturn(List.of(
                BaseTask.builder().id(21L).orgId(1L).name("绘制").status(Status.ENABLED).metaSchema("[]").build(),
                BaseTask.builder().id(22L).orgId(2L).name("组织B机密任务").status(Status.ENABLED).metaSchema("[{\"key\":\"secret\"}]").build()));

        assertThatThrownBy(() -> service.createProjectItem(ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build()))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());

        verify(itemMapper, never()).insert(any(Item.class));
        verifyNoInteractions(itemTaskFlowMapper, itemTaskNodeMapper, taskInstanceMapper);
        verify(taskTemplateMapper, never()).update(isNull(), any());
    }


    @Test
    void deleteProjectItemForReview_alreadyDeleted_rejectsRepeatOperation() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Item.class);
        when(itemMapper.selectById(2L)).thenReturn(Item.builder().id(2L).projectId(1L).build());
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(TaskInstance.builder().id(8L).build()));
        when(itemMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.deleteProjectItemForReview(1L, 2L, 1L))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verify(itemTaskFlowMapper, never()).update(isNull(), any());
        verify(itemTaskNodeMapper, never()).update(isNull(), any());
        verify(taskInstanceMapper, never()).update(isNull(), any());
        verify(taskTemplateMapper, never()).update(isNull(), any());
        verify(resourceService, never()).abandonBound(any());
    }

    @Test
    void deleteProjectItemForReview_cascadesSoftDeleteToFlowNodeAndInstance() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Item.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ItemTaskFlow.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ItemTaskNode.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskInstance.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskTemplate.class);
        when(itemMapper.selectById(2L)).thenReturn(
                Item.builder().id(2L).projectId(1L).taskTemplateId(9L).build());
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(
                TaskInstance.builder().id(8L).itemId(2L).build(),
                TaskInstance.builder().id(9L).itemId(2L).build()));
        when(itemMapper.update(isNull(), any())).thenReturn(1);
        when(itemTaskFlowMapper.update(isNull(), any())).thenReturn(1);
        when(itemTaskNodeMapper.update(isNull(), any())).thenReturn(2);
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(2);
        when(taskTemplateMapper.update(isNull(), any())).thenReturn(1);

        service.deleteProjectItemForReview(1L, 2L, 1L);

        verify(itemMapper).update(isNull(), any());
        verify(itemTaskFlowMapper).update(isNull(), any());
        verify(itemTaskNodeMapper).update(isNull(), any());
        verify(taskInstanceMapper).update(isNull(), any());
        verify(taskTemplateMapper).update(isNull(), any());
        ArgumentCaptor<ResourceAbandonParamsBO> abandoned = ArgumentCaptor.forClass(ResourceAbandonParamsBO.class);
        verify(resourceService).abandonBound(abandoned.capture());
        assertThat(abandoned.getValue().getOrgId()).isEqualTo(1L);
        assertThat(abandoned.getValue().getBizType()).isEqualTo("task_submit");
        assertThat(abandoned.getValue().getBizIds()).containsExactly(8L, 9L);
    }



    @Test
    void reorderProjectItems_updatesOnlyChangedPositions() {
        when(itemMapper.selectList(any())).thenReturn(List.of(
                orderedItem(1L, 1L), orderedItem(2L, 2L), orderedItem(3L, 3L),
                orderedItem(4L, 4L), orderedItem(5L, 5L)));

        service.reorderProjectItems(reorderParams(List.of(1L, 2L, 3L, 4L, 5L), List.of(1L, 5L, 2L, 3L, 4L)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ItemSortUpdateBO>> updates = ArgumentCaptor.forClass(List.class);
        verify(itemMapper).updateSorts(org.mockito.ArgumentMatchers.eq(1L), updates.capture());
        assertThat(updates.getValue()).extracting(ItemSortUpdateBO::getId).containsExactly(5L, 2L, 3L, 4L);
        assertThat(updates.getValue()).extracting(ItemSortUpdateBO::getSort).containsExactly(2L, 3L, 4L, 5L);
    }

    @Test
    void reorderProjectItems_rejectsStaleOrderWithoutWriting() {
        when(itemMapper.selectList(any())).thenReturn(List.of(
                orderedItem(1L, 2L), orderedItem(2L, 1L), orderedItem(3L, 3L)));

        assertThatThrownBy(() -> service.reorderProjectItems(
                reorderParams(List.of(1L, 2L, 3L), List.of(1L, 3L, 2L))))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verify(itemMapper, never()).updateSorts(any(), any());
    }

    @Test
    void reorderProjectItems_rejectsMembershipChangesWithoutWriting() {
        when(itemMapper.selectList(any())).thenReturn(List.of(orderedItem(1L, 1L)));
        assertThatThrownBy(() -> service.reorderProjectItems(reorderParams(List.of(1L, 2L), List.of(2L, 1L))))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verify(itemMapper, never()).updateSorts(any(), any());
    }

    @Test
    void reorderProjectItems_acceptsAlreadySavedOrderWithoutRewriting() {
        when(itemMapper.selectList(any())).thenReturn(List.of(orderedItem(1L, 2L), orderedItem(2L, 1L)));
        service.reorderProjectItems(reorderParams(List.of(1L, 2L), List.of(2L, 1L)));
        verify(itemMapper, never()).updateSorts(any(), any());
    }

    @Test
    void reorderProjectItems_rejectsDuplicateIds() {
        assertThatThrownBy(() -> service.reorderProjectItems(reorderParams(List.of(1L, 2L), List.of(1L, 1L))))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
        verifyNoInteractions(itemMapper);
    }

    private Item orderedItem(Long id, Long sort) {
        return Item.builder().id(id).projectId(1L).sort(sort).build();
    }

    private ItemReorderParamsBO reorderParams(List<Long> expected, List<Long> requested) {
        return ItemReorderParamsBO.builder().projectId(1L).orgId(1L).operatorId(1L)
                .expectedItemIds(expected).itemIds(requested).build();
    }

    private Project disabledProject() {
        return Project.builder().id(1L).orgId(1L).creatorId(1L).resourceStatus(Status.DISABLED).build();
    }
}
