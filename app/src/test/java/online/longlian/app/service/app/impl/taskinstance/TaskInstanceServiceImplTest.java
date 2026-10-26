package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
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
import online.longlian.app.pojo.bo.app.TaskInstanceAssignParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitResultBO;
import online.longlian.app.pojo.bo.app.NotificationBatchCreateParamsBO;
import online.longlian.app.pojo.bo.app.NotificationCreateBO;
import online.longlian.app.service.app.InboxNotificationService;
import online.longlian.common.enumeration.InboxNotificationKind;
import online.longlian.common.enumeration.TaskInstanceStatus;
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
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.UserOperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskInstanceServiceImplTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private OrganizationMemberMapper organizationMemberMapper;
    @Mock
    private UserMapper userMapper;
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
    private PlatformTransactionManager transactionManager;
    @Mock
    private ProjectService projectService;
    @Mock
    private InboxNotificationService inboxNotificationService;

    private TaskInstanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskInstanceServiceImpl(taskInstanceMapper, organizationMemberMapper, userMapper,
                taskSubmissionMapper,
                itemMapper, projectMapper, itemTaskNodeMapper,
                taskInstanceAssembler, taskInstanceCommandHandler, operationLogService,
                itemTaskFlowMapper, organizationMembershipService, transactionManager, projectService,
                inboxNotificationService, Clock.fixed(Instant.parse("2026-10-26T01:00:00Z"), ZoneId.of("UTC")));
        lenient().when(itemTaskFlowMapper.selectOne(any())).thenReturn(ItemTaskFlow.builder()
                .id(6L).itemId(1L).projectId(2L).build());
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
    void shouldAssignPendingTaskAndNotifyAssignee() {
        TaskInstanceAssignParamsBO params = prepareAssign(1L);
        when(projectService.addToWorkshop(any())).thenReturn(new ProjectWorkshopAddResultBO(true));

        TaskClaimResultBO result = service.assignTask(params);

        assertThat(result.isAutoJoinedProject()).isTrue();
        verify(organizationMembershipService).requireEnabledMember(20L, 10L);
        verify(taskInstanceCommandHandler).assign(any(TaskInstance.class), eq(20L), eq("0"));
        ArgumentCaptor<ProjectWorkshopAddParamsBO> membership = ArgumentCaptor.forClass(ProjectWorkshopAddParamsBO.class);
        verify(projectService).addToWorkshop(membership.capture());
        assertThat(membership.getValue().getUserId()).isEqualTo(20L);
        ArgumentCaptor<NotificationBatchCreateParamsBO> notifications =
                ArgumentCaptor.forClass(NotificationBatchCreateParamsBO.class);
        verify(inboxNotificationService).createBatch(notifications.capture());
        NotificationCreateBO notification = notifications.getValue().getNotifications().get(0);
        assertThat(notification.getRecipientUserId()).isEqualTo(20L);
        assertThat(notification.getKind()).isEqualTo(InboxNotificationKind.TASK_ASSIGNED);
        assertThat(notification.getSourceId()).isEqualTo(100L);
        assertThat(notification.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-10-26T01:00:00"));
        verify(operationLogService).logSynchronously(eq(1L), eq(2L), eq(1L), eq(UserOperationType.TASK_ASSIGN), any());
    }

    @Test
    void shouldRejectAssignmentByNonCreator() {
        TaskInstanceAssignParamsBO params = prepareAssign(99L);

        assertThatThrownBy(() -> service.assignTask(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("仅企划负责人可分配任务");
        verify(taskInstanceCommandHandler, never()).assign(any(), anyLong(), any());
        verifyNoInteractions(inboxNotificationService);
    }

    @Test
    void shouldRejectAssignmentOutsideOrganization() {
        TaskInstanceAssignParamsBO params = prepareAssign(1L);
        doThrow(new AppException(ResultCode.OPERATION_FAIL, "您不是该组织成员"))
                .when(organizationMembershipService).requireEnabledMember(20L, 10L);
        lenient().when(organizationMembershipService.requireEnabledMember(1L, 10L))
                .thenReturn(online.longlian.app.pojo.entity.OrganizationMember.builder().userId(1L).build());

        assertThatThrownBy(() -> service.assignTask(params)).isInstanceOf(AppException.class);
        verify(taskInstanceCommandHandler, never()).assign(any(), anyLong(), any());
        verifyNoInteractions(inboxNotificationService, projectService);
    }

    @Test
    void shouldNotNotifyWhenAssignedTaskAlreadyClaimed() {
        TaskInstanceAssignParamsBO params = prepareAssign(1L);
        doThrow(new AppException(ResultCode.OPERATION_FAIL, "该任务不可分配"))
                .when(taskInstanceCommandHandler).assign(any(), eq(20L), eq("0"));

        assertThatThrownBy(() -> service.assignTask(params)).isInstanceOf(AppException.class);
        verifyNoInteractions(inboxNotificationService, operationLogService);
    }


    private TaskInstanceAssignParamsBO prepareAssign(Long operatorId) {
        TaskInstance instance = prepareReviewInstance();
        instance.setStatus(TaskInstanceStatus.PENDING);
        when(projectMapper.selectById(2L)).thenReturn(Project.builder()
                .id(2L).orgId(10L).creatorId(1L).resourceStatus(Status.ENABLED).build());
        return TaskInstanceAssignParamsBO.builder()
                .instanceId(100L).orgId(10L).userId(operatorId).assigneeId(20L).expectedRevision("0").build();
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

        verify(taskInstanceCommandHandler).abandon(instance, 1L, false, "0");
        verify(operationLogService).log(1L, instance.getProjectId(), instance.getItemId(),
                UserOperationType.TASK_ABANDON, params);
    }

    @Test
    void abandonTask_creatorCancelsOtherAssignee_writesSynchronousAuditWithoutReason() {
        TaskInstanceAbandonParamsBO params = TaskInstanceAbandonParamsBO.builder()
                .instanceId(100L).orgId(10L).userId(1L).expectedRevision("0").build();
        TaskInstance instance = prepareReviewInstance();
        instance.setAssigneeId(2L);
        Project project = enabledProject();
        project.setCreatorId(1L);
        when(projectMapper.selectById(2L)).thenReturn(project);

        service.abandonTask(params);

        verify(taskInstanceCommandHandler).abandon(instance, 1L, true, "0");
        verify(operationLogService).logSynchronously(1L, 2L, 1L, UserOperationType.TASK_ABANDON,
                Map.of("action", "OWNER_CANCEL", "instanceId", 100L, "assigneeId", 2L,
                        "expectedRevision", "0", "revision", "1"));
        verifyNoMoreInteractions(operationLogService);
    }

    @Test
    void abandonTask_creatorAbandonsOwnClaim_keepsOrdinaryLog() {
        TaskInstanceAbandonParamsBO params = TaskInstanceAbandonParamsBO.builder()
                .instanceId(100L).orgId(10L).userId(1L).expectedRevision("0").build();
        TaskInstance instance = prepareReviewInstance();
        instance.setAssigneeId(1L);
        Project project = enabledProject();
        project.setCreatorId(1L);
        when(projectMapper.selectById(2L)).thenReturn(project);

        service.abandonTask(params);

        verify(taskInstanceCommandHandler).abandon(instance, 1L, true, "0");
        verify(operationLogService).log(1L, 2L, 1L, UserOperationType.TASK_ABANDON, params);
        verifyNoMoreInteractions(operationLogService);
    }

    @Test
    void shouldNotNotifyUntilLastParallelPredecessorCompletes() {
        var before = List.of(progressNode(11L, 1, false), progressNode(12L, 1, false),
                progressNode(21L, 2, false));
        var after = List.of(progressNode(11L, 1, true), progressNode(12L, 1, false),
                progressNode(21L, 2, false));
        service.submitTask(prepareSubmit(before, after));
        assertThat(capturedNotifications().getNotifications()).isEmpty();
        verify(taskInstanceMapper, never()).selectList(any());
    }

    @Test
    void shouldOmitEverySlotClaimedBySubmitter() {
        var before = List.of(progressNode(11L, 1, false), progressNode(12L, 1, true),
                progressNode(21L, 2, false), progressNode(22L, 2, false));
        var after = List.of(progressNode(11L, 1, true), progressNode(12L, 1, true),
                progressNode(21L, 2, false), progressNode(22L, 2, false));
        var params = prepareSubmit(before, after);
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(
                TaskInstance.builder().id(201L).assigneeId(1L).build(),
                TaskInstance.builder().id(202L).assigneeId(1L).build()));
        service.submitTask(params);
        assertThat(capturedNotifications().getNotifications()).isEmpty();
    }

    @Test
    void shouldKeepOnlyOtherAssigneesWhenSubmitterAlsoClaimedASlot() {
        var before = List.of(progressNode(11L, 1, false), progressNode(12L, 1, true),
                progressNode(21L, 2, false), progressNode(22L, 2, false));
        var after = List.of(progressNode(11L, 1, true), progressNode(12L, 1, true),
                progressNode(21L, 2, false), progressNode(22L, 2, false));
        var params = prepareSubmit(before, after);
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(
                TaskInstance.builder().id(201L).assigneeId(1L).build(),
                TaskInstance.builder().id(202L).assigneeId(4L).build()));
        service.submitTask(params);
        assertThat(capturedNotifications().getNotifications()).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientUserId()).isEqualTo(4L);
            assertThat(notification.getSourceId()).isEqualTo(202L);
            assertThat(notification.getKind()).isEqualTo(InboxNotificationKind.TASK_READY);
        });
    }

    @Test
    void shouldReunlockDistantClaimedStageAfterEarlyReset() {
        var before = List.of(progressNode(11L, 1, false), progressNode(21L, 2, true),
                progressNode(31L, 3, true), progressNode(41L, 4, false));
        var after = List.of(progressNode(11L, 1, true), progressNode(21L, 2, true),
                progressNode(31L, 3, true), progressNode(41L, 4, false));
        var params = prepareSubmit(before, after);
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(
                TaskInstance.builder().id(401L).assigneeId(4L).build()));
        service.submitTask(params);
        assertThat(capturedNotifications().getNotifications())
                .extracting(NotificationCreateBO::getSourceId).containsExactly(401L);
    }

    @Test
    void shouldNotifyPlanningCreatorOnlyWhenItemBecomesFullyComplete() {
        var params = prepareSubmit(List.of(progressNode(11L, 1, true), progressNode(21L, 2, false)),
                List.of(progressNode(11L, 1, true), progressNode(21L, 2, true)));
        service.submitTask(params);
        assertThat(capturedNotifications().getNotifications()).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientUserId()).isEqualTo(99L);
            assertThat(notification.getKind()).isEqualTo(InboxNotificationKind.ITEM_COMPLETED);
            assertThat(notification.getSourceId()).isEqualTo(1L);
            assertThat(notification.getTriggerId()).isEqualTo(901L);
        });
    }

    @Test
    void shouldOmitItemCompletionWhenPlanningCreatorSubmitsLastTask() {
        var params = prepareSubmit(List.of(progressNode(11L, 1, true), progressNode(21L, 2, false)),
                List.of(progressNode(11L, 1, true), progressNode(21L, 2, true)));
        var project = enabledProject();
        project.setCreatorId(1L);
        when(projectMapper.selectById(2L)).thenReturn(project);
        service.submitTask(params);
        assertThat(capturedNotifications().getNotifications()).isEmpty();
    }

    private ItemStageNodeStatusBO progressNode(Long id, int sort, boolean completed) {
        return new ItemStageNodeStatusBO(1L, id, "任务", sort, 1, completed);
    }

    private TaskInstanceSubmitParamsBO prepareSubmit(List<ItemStageNodeStatusBO> before,
                                                    List<ItemStageNodeStatusBO> after) {
        var instance = TaskInstance.builder().revision(0L).id(100L).projectId(2L).itemId(1L).build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(itemMapper.selectById(1L)).thenReturn(writableItem());
        var project = enabledProject();
        project.setCreatorId(99L);
        when(projectMapper.selectById(2L)).thenReturn(project);
        when(taskInstanceCommandHandler.submit(eq(instance), eq(1L), eq(10L), anyMap()))
                .thenReturn(new TaskInstanceSubmitResultBO(901L, LocalDateTime.of(2026, 10, 22, 12, 0), before));
        when(taskInstanceMapper.selectItemStageNodeStatuses(List.of(1L))).thenReturn(after);
        return TaskInstanceSubmitParamsBO.builder().instanceId(100L).orgId(10L).userId(1L).values(Map.of()).build();
    }

    private NotificationBatchCreateParamsBO capturedNotifications() {
        var captor = ArgumentCaptor.forClass(NotificationBatchCreateParamsBO.class);
        verify(inboxNotificationService).createBatch(captor.capture());
        return captor.getValue();
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
