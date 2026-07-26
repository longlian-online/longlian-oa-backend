package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.service.app.impl.UserOperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

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

    private TaskInstanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskInstanceServiceImpl(taskInstanceMapper, taskSubmissionMapper,
                itemMapper, projectMapper, itemTaskNodeMapper,
                taskInstanceAssembler, taskInstanceCommandHandler, operationLogService);
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
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());
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
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());

        TaskInstance instance = TaskInstance.builder().id(100L).itemTaskNodeId(5L).build();
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(instance));
        ItemTaskNode node = ItemTaskNode.builder().id(5L).build();
        when(itemTaskNodeMapper.selectBatchIds(anyList())).thenReturn(List.of(node));
        when(taskInstanceAssembler.assembleInstances(anyList(), anyMap())).thenReturn(List.of(new ItemTaskInstanceVO()));

        List<ItemTaskInstanceVO> result = service.listItemTaskInstances(params);

        assertThat(result).hasSize(1);
    }

    @Test
    void claimTask_validInstance_delegatesToHandler() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        params.setUserId(1L);

        TaskInstance instance = TaskInstance.builder().id(100L).projectId(2L).itemId(1L).build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());

        service.claimTask(params);

        verify(taskInstanceCommandHandler).claim(instance, 1L);
        verify(operationLogService).log(eq(1L), eq(2L), eq(1L), any(), eq(params));
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
        when(taskInstanceMapper.selectById(100L)).thenReturn(TaskInstance.builder().id(100L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());

        assertThatThrownBy(() -> service.claimTask(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("无权操作该任务");
    }

    @Test
    void abandonTask_validInstance_delegatesToHandler() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        params.setUserId(1L);

        TaskInstance instance = TaskInstance.builder().id(100L).projectId(2L).itemId(1L).build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());

        service.abandonTask(params);

        verify(taskInstanceCommandHandler).abandon(instance, 1L);
    }

    @Test
    void submitTask_validInstance_delegatesToHandler() {
        TaskInstanceSubmitParamsBO params = new TaskInstanceSubmitParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        params.setUserId(1L);
        params.setMetadata("{\"key\":\"value\"}");

        TaskInstance instance = TaskInstance.builder().id(100L).projectId(2L).itemId(1L).build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());

        service.submitTask(params);

        verify(taskInstanceCommandHandler).submit(instance, 1L, "{\"key\":\"value\"}");
    }

    @Test
    void resetTask_validInstance_delegatesToHandler() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        params.setUserId(1L);

        TaskInstance instance = TaskInstance.builder().id(100L).projectId(2L).itemId(1L).build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());

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

        TaskInstance instance = TaskInstance.builder().id(100L).projectId(2L).itemId(1L).build();
        when(taskInstanceMapper.selectById(100L)).thenReturn(instance);
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());

        service.rejectTask(params);

        verify(taskInstanceCommandHandler).reject(instance, 2L, "需要修改");
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
        when(taskInstanceMapper.selectById(100L)).thenReturn(TaskInstance.builder().id(100L).itemId(1L).build());
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());

        assertThatThrownBy(() -> service.getTaskInstanceDetail(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("任务实例不存在");
    }

    @Test
    void getTaskInstanceDetail_withSubmission_returnsMetadata() {
        TaskInstanceDetailParamsBO params = new TaskInstanceDetailParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(TaskInstance.builder().id(100L).itemId(1L).build());
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());
        TaskSubmission submission = TaskSubmission.builder().metadata("{\"data\":1}").build();
        when(taskSubmissionMapper.selectOne(any())).thenReturn(submission);

        TaskInstanceDetailVO result = service.getTaskInstanceDetail(params);

        assertThat(result.getMetadata()).isEqualTo("{\"data\":1}");
    }

    @Test
    void getTaskInstanceDetail_noSubmission_returnsEmptyVO() {
        TaskInstanceDetailParamsBO params = new TaskInstanceDetailParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(TaskInstance.builder().id(100L).itemId(1L).build());
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(10L).build());
        when(taskSubmissionMapper.selectOne(any())).thenReturn(null);

        TaskInstanceDetailVO result = service.getTaskInstanceDetail(params);

        assertThat(result.getMetadata()).isNull();
    }

    @Test
    void getAndValidateInstance_projectNull_throws() {
        TaskInstanceOperateParamsBO params = new TaskInstanceOperateParamsBO();
        params.setInstanceId(100L);
        params.setOrgId(10L);
        when(taskInstanceMapper.selectById(100L)).thenReturn(TaskInstance.builder().id(100L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(null);

        assertThatThrownBy(() -> service.claimTask(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("任务实例不存在");
    }
}
