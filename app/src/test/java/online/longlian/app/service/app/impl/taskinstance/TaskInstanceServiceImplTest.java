package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddResultBO;
import online.longlian.app.pojo.bo.app.TaskClaimResultBO;
import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceAbandonParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ItemTaskFlow;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.app.impl.UserOperationLogService;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.common.service.DistributedLockService;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskInstanceServiceImplTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private TaskSubmissionMapper taskSubmissionMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ItemTaskNodeMapper itemTaskNodeMapper;
    @Mock
    private TaskInstanceAssembler taskInstanceAssembler;
    @Mock
    private TaskInstanceCommandHandler taskInstanceCommandHandler;
    @Mock
    private UserOperationLogService operationLogService;
    @Mock
    private ItemTaskFlowMapper itemTaskFlowMapper;
    @Mock
    private OrganizationMembershipService organizationMembershipService;
    @Mock
    private LockService lockService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private ProjectService projectService;

    private TaskInstanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskInstanceServiceImpl(taskInstanceMapper, taskSubmissionMapper,
                itemMapper, projectMapper, itemTaskNodeMapper,
                taskInstanceAssembler, taskInstanceCommandHandler, operationLogService,
                itemTaskFlowMapper, organizationMembershipService, lockService, transactionManager, projectService);
        lenient().when(lockService.tryAcquireOrThrow(anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(mock(DistributedLockService.Lock.class));
        lenient().when(transactionManager.getTransaction(any()))
                .thenAnswer(invocation -> new SimpleTransactionStatus());
    }

    @Test
    void listItemTaskInstances_itemNotFound_throws() {
        TaskInstanceListParamsBO params = new TaskInstanceListParamsBO();
        params.setItemId(1L);
        params.setOrgId(10L);
        when(itemMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.listItemTaskInstances(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("项目不存在");
    }

    @Test
    void listItemTaskInstances_projectNotInOrg_throws() {
        TaskInstanceListParamsBO params = new TaskInstanceListParamsBO();
        params.setItemId(1L);
        params.setOrgId(10L);
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(99L).build());

        assertThatThrownBy(() -> service.listItemTaskInstances(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("项目不存在");
    }

    @Test
    void listItemTaskInstances_noInstances_returnsEmpty() {
        TaskInstanceListParamsBO params = new TaskInstanceListParamsBO();
        params.setItemId(1L);
        params.setOrgId(10L);
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());
        when(taskInstanceMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<ItemTaskInstanceVO> result = service.listItemTaskInstances(params);

        assertThat(result).isEmpty();
    }

    @Test
    void listItemTaskInstances_withInstances_returnsAssembled() {
        TaskInstanceListParamsBO params = new TaskInstanceListParamsBO();
        params.setItemId(1L);
        params.setOrgId(10L);
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());

        TaskInstance instance = TaskInstance.builder().revision(0L).id(100L).itemTaskNodeId(5L).build();
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(instance));
        ItemTaskNode node = ItemTaskNode.builder().id(5L).build();
        when(itemTaskNodeMapper.selectBatchIds(anyList())).thenReturn(List.of(node));
        when(taskInstanceAssembler.assembleInstances(anyList(), anyMap())).thenReturn(List.of(new ItemTaskInstanceVO()));

        List<ItemTaskInstanceVO> result = service.listItemTaskInstances(params);

        assertThat(result).hasSize(1);
    }

    /** 仅在 CAS 接取成功后加入企划，返回所属服务确认的实际新增结果。 */
    @Test
    void shouldAutoJoinProjectAfterClaimSucceeds() {
        TaskInstanceOperateParamsBO params = prepareClaim();
        when(projectService.addToWorkshop(any())).thenReturn(new ProjectWorkshopAddResultBO(true));

        TaskClaimResultBO result = service.claimTask(params);

        assertThat(result.isAutoJoinedProject()).isTrue();
        var order = inOrder(taskInstanceCommandHandler, projectService, operationLogService);
        order.verify(taskInstanceCommandHandler).claim(any(TaskInstance.class), eq(1L));
        ArgumentCaptor<ProjectWorkshopAddParamsBO> membership = ArgumentCaptor.forClass(ProjectWorkshopAddParamsBO.class);
        order.verify(projectService).addToWorkshop(membership.capture());
        assertThat(membership.getValue().getProjectId()).isEqualTo(2L);
        assertThat(membership.getValue().getUserId()).isEqualTo(1L);
        assertThat(membership.getValue().getOrgId()).isEqualTo(10L);
        order.verify(operationLogService).log(eq(1L), eq(2L), eq(1L), any(), eq(params));
    }

    /** 已加入时沿用成功接取流程，但不能误报本次新增。 */
    @Test
    void shouldReturnFalseWhenProjectAlreadyJoined() {
        TaskInstanceOperateParamsBO params = prepareClaim();
        when(projectService.addToWorkshop(any())).thenReturn(new ProjectWorkshopAddResultBO(false));

        assertThat(service.claimTask(params).isAutoJoinedProject()).isFalse();
        verify(operationLogService).log(eq(1L), eq(2L), eq(1L), any(), eq(params));
    }

    /** 重复接取或 CAS 竞争失败不得执行加入或成功日志。 */
    @Test
    void shouldNotJoinProjectWhenClaimFails() {
        TaskInstanceOperateParamsBO params = prepareClaim();
        doThrow(new AppException(ResultCode.OPERATION_FAIL, "该任务状态已变更"))
                .when(taskInstanceCommandHandler).claim(any(), eq(1L));

        assertThatThrownBy(() -> service.claimTask(params)).isInstanceOf(AppException.class);
        verifyNoInteractions(projectService, operationLogService);
    }

    /** 加入失败必须继续向事务边界抛出异常，不能返回接取成功。 */
    @Test
    void shouldPropagateMembershipFailureWithoutSuccessResult() {
        TaskInstanceOperateParamsBO params = prepareClaim();
        when(projectService.addToWorkshop(any())).thenThrow(new IllegalStateException("membership write failed"));

        assertThatThrownBy(() -> service.claimTask(params)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(operationLogService);
    }

    @Test
    void claimTask_instanceNotFound_throws() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(999L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.claimTask(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("任务实例不存在");
    }

    @Test
    void claimTask_wrongOrg_throws() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(99L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(TaskInstance.builder().revision(0L).id(100L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());

        assertThatThrownBy(() -> service.claimTask(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("无权操作该任务");
    }

    @Test
    void abandonTask_validInstance_delegatesToHandler() {
        TaskInstanceAbandonParamsBO params = new TaskInstanceAbandonParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        params.setUserId(1L);
        params.setExpectedRevision("0");

        TaskInstance instance = prepareReviewInstance();

        service.abandonTask(params);

        verify(taskInstanceCommandHandler).abandon(instance, 1L, false, "0", null);
    }

    @Test
    void submitTask_validInstance_delegatesToHandler() {
        TaskInstanceSubmitParamsBO params = new TaskInstanceSubmitParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        params.setUserId(1L);
        params.setValues(Map.of("key", "value"));

        TaskInstance instance = TaskInstance.builder().revision(0L).id(100L).projectId(2L).itemId(1L).build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());
        when(itemMapper.selectById(1L)).thenReturn(writableItem());

        service.submitTask(params);

        verify(taskInstanceCommandHandler).submit(instance, 1L, 10L, Map.of("key", "value"));
    }

    @Test
    void resetTask_validInstance_delegatesToHandler() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        params.setUserId(1L);

        TaskInstance instance = TaskInstance.builder().revision(0L).id(100L).projectId(2L).itemId(1L).build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());
        when(itemMapper.selectById(1L)).thenReturn(writableItem());

        service.resetTask(params);

        verify(taskInstanceCommandHandler).reset(instance, 1L);
    }

    @Test
    void rejectTask_validInstance_delegatesToHandler() {
        TaskInstanceRejectParamsBO params = new TaskInstanceRejectParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        params.setUserId(2L);
        params.setReviewComment("需要修改");
        params.setExpectedRevision("0");

        TaskInstance instance = prepareReviewInstance();

        service.rejectTask(params);

        verify(taskInstanceCommandHandler).reject(instance, 2L, false, "0", "需要修改");
    }

    @Test
    void shouldAuthorizeCreatorAndSynchronouslyAuditRejectWithinTransaction() {
        TaskInstance instance = prepareReviewInstance();
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L)
                .creatorId(1L).resourceStatus(Status.ENABLED).build());
        TaskInstanceRejectParamsBO params = TaskInstanceRejectParamsBO.builder()
                .instanceId(100L).orgId(10L).userId(1L).expectedRevision("0").reviewComment("修改").build();
        service.rejectTask(params);
        var order = inOrder(organizationMembershipService, taskInstanceCommandHandler, operationLogService, transactionManager);
        order.verify(organizationMembershipService).requireEnabledMember(1L, 10L);
        order.verify(taskInstanceCommandHandler).reject(instance, 1L, true, "0", "修改");
        order.verify(operationLogService).logSynchronously(eq(1L), eq(2L), eq(1L), any(), any());
        order.verify(transactionManager).commit(any());
        verify(operationLogService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void getTaskInstanceDetail_instanceNotFound_throws() {
        TaskInstanceDetailParamsBO params = new TaskInstanceDetailParamsBO();
        params.setInstanceId(999L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getTaskInstanceDetail(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("任务实例不存在");
    }

    @Test
    void getTaskInstanceDetail_projectNotInOrg_throws() {
        TaskInstanceDetailParamsBO params = new TaskInstanceDetailParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(99L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(TaskInstance.builder().revision(0L).id(100L).itemId(1L).build());
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());

        assertThatThrownBy(() -> service.getTaskInstanceDetail(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("任务实例不存在");
        verifyNoInteractions(taskSubmissionMapper, taskInstanceAssembler);
    }

    @Test
    void getTaskInstanceDetail_withSubmission_assemblesAuthorizedDetail() {
        TaskInstanceDetailParamsBO params = new TaskInstanceDetailParamsBO(100L, 10L);
        TaskInstance instance = TaskInstance.builder().revision(0L).id(100L).itemId(1L).itemTaskNodeId(3L).build();
        ItemTaskNode node = ItemTaskNode.builder().id(3L).metaSchema("[]").build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());
        when(itemTaskNodeMapper.selectById(3L)).thenReturn(node);
        TaskSubmission submission = TaskSubmission.builder().metadata("{}").build();
        when(taskSubmissionMapper.selectOne(any())).thenReturn(submission);
        TaskInstanceDetailVO expected = new TaskInstanceDetailVO();
        when(taskInstanceAssembler.assembleDetail(instance, node, submission, 10L)).thenReturn(expected);

        assertThat(service.getTaskInstanceDetail(params)).isSameAs(expected);
    }

    @Test
    void getTaskInstanceDetail_noSubmission_assemblesTaskWithoutSubmission() {
        TaskInstanceDetailParamsBO params = new TaskInstanceDetailParamsBO(100L, 10L);
        TaskInstance instance = TaskInstance.builder().revision(0L).id(100L).itemId(1L).itemTaskNodeId(3L).build();
        ItemTaskNode node = ItemTaskNode.builder().id(3L).metaSchema("[]").build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());
        when(itemTaskNodeMapper.selectById(3L)).thenReturn(node);
        TaskInstanceDetailVO expected = new TaskInstanceDetailVO();
        when(taskInstanceAssembler.assembleDetail(instance, node, null, 10L)).thenReturn(expected);

        assertThat(service.getTaskInstanceDetail(params)).isSameAs(expected);
    }

    @Test
    void getAndValidateInstance_projectNull_throws() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(TaskInstance.builder().revision(0L).id(100L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(null);

        assertThatThrownBy(() -> service.claimTask(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("任务实例不存在");
    }

    @Test
    void listItemTaskInstances_disabledProject_throws() {
        TaskInstanceListParamsBO params = new TaskInstanceListParamsBO();
        params.setItemId(1L);
        params.setOrgId(10L);
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(disabledProject());

        assertThatThrownBy(() -> service.listItemTaskInstances(params)).isInstanceOf(AppException.class);
        verify(taskInstanceMapper, never()).selectList(any());
    }

    @Test
    void getTaskInstanceDetail_disabledProject_throws() {
        TaskInstanceDetailParamsBO params = new TaskInstanceDetailParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(TaskInstance.builder().revision(0L).id(100L).itemId(1L).build());
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(disabledProject());

        assertThatThrownBy(() -> service.getTaskInstanceDetail(params)).isInstanceOf(AppException.class);
        verify(taskSubmissionMapper, never()).selectOne(any());
    }

    @Test
    void claimTask_disabledProject_throws() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(
                TaskInstance.builder().revision(0L).id(100L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(disabledProject());

        assertThatThrownBy(() -> service.claimTask(params)).isInstanceOf(AppException.class);
        verify(taskInstanceCommandHandler, never()).claim(any(), anyLong());
    }

    @Test
    void claimTask_deletedItem_throws() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(
                TaskInstance.builder().revision(0L).id(100L).projectId(2L).itemId(1L).build());
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());
        when(itemMapper.selectById(1L)).thenReturn(
                Item.builder().id(1L).projectId(2L)
                        .deletedAt(LocalDateTime.of(2026, 10, 1, 0, 0)).build());

        assertThatThrownBy(() -> service.claimTask(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("项目已删除");
        verify(taskInstanceCommandHandler, never()).claim(any(), anyLong());
    }


    @Test
    void claimTask_itemMissing_throws() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(
                TaskInstance.builder().revision(0L).id(100L).projectId(2L).itemId(1L).build());
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());
        when(itemMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.claimTask(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("项目不存在");
        verify(taskInstanceCommandHandler, never()).claim(any(), anyLong());
    }


    @Test
    void resetTask_deletedItem_throws() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(
                TaskInstance.builder().revision(0L).id(100L).projectId(2L).itemId(1L).build());
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());
        when(itemMapper.selectById(1L)).thenReturn(
                Item.builder().id(1L).projectId(2L)
                        .deletedAt(LocalDateTime.of(2026, 10, 1, 0, 0)).build());

        assertThatThrownBy(() -> service.resetTask(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("项目已删除");
        verify(taskInstanceCommandHandler, never()).reset(any(), anyLong());
    }


    private TaskInstanceOperateParamsBO prepareClaim() {
        TaskInstanceOperateParamsBO params = TaskInstanceOperateParamsBO.builder()
                .instanceId(100L).orgId(10L).userId(1L).build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(
                TaskInstance.builder().id(100L).projectId(2L).itemId(1L).build());
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());
        when(itemMapper.selectById(1L)).thenReturn(writableItem());
        return params;
    }

    private TaskInstance prepareReviewInstance() {
        TaskInstance instance = TaskInstance.builder().revision(0L).id(100L).projectId(2L).itemId(1L)
                .itemTaskNodeId(5L).taskFlowId(6L).build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(projectMapper.selectById(2L)).thenReturn(enabledProject());
        when(itemMapper.selectById(1L)).thenReturn(writableItem());
        when(itemTaskNodeMapper.selectById(5L)).thenReturn(ItemTaskNode.builder()
                .id(5L).projectId(2L).itemId(1L).itemTaskFlowId(6L).build());
        when(itemTaskFlowMapper.selectById(6L)).thenReturn(ItemTaskFlow.builder()
                .id(6L).projectId(2L).itemId(1L).build());
        return instance;
    }

    private Item writableItem() {
        return Item.builder().id(1L).projectId(2L).build();
    }

    private Project enabledProject() {
        return Project.builder().id(2L).orgId(10L).resourceStatus(Status.ENABLED).build();
    }

    private Project disabledProject() {
        return Project.builder().id(2L).orgId(10L).resourceStatus(Status.DISABLED).build();
    }
}
