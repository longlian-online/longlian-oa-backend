package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskInstanceCommandHandlerTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private TaskSubmissionMapper taskSubmissionMapper;
    @Mock
    private ItemTaskNodeMapper itemTaskNodeMapper;
    @Mock
    private MemberSubmitCountHandler memberSubmitCountHandler;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
    private TaskInstanceCommandHandler handler;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskInstance.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskSubmission.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ItemTaskNode.class);
        handler = new TaskInstanceCommandHandler(taskInstanceMapper, taskSubmissionMapper,
                itemTaskNodeMapper, memberSubmitCountHandler, clock);
    }

    @Test
    void claim_pendingInstance_succeeds() {
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.PENDING).build();
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(1);

        handler.claim(instance, 10L);

        verify(taskInstanceMapper).update(isNull(), any());
    }

    @Test
    void claim_nonPendingInstance_throws() {
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.CLAIMED).build();

        assertThatThrownBy(() -> handler.claim(instance, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("该任务不可接取");
    }

    @Test
    void claim_concurrentUpdate_throws() {
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.PENDING).build();
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> handler.claim(instance, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("该任务状态已变更");
    }

    @Test
    void abandon_claimedBySameUser_succeeds() {
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.CLAIMED).assigneeId(10L).build();
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(1);

        handler.abandon(instance, 10L);

        verify(taskInstanceMapper).update(isNull(), any());
    }

    @Test
    void abandon_notClaimed_throws() {
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.PENDING).build();

        assertThatThrownBy(() -> handler.abandon(instance, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("该任务不可放弃");
    }

    @Test
    void abandon_wrongUser_throws() {
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.CLAIMED).assigneeId(99L).build();

        assertThatThrownBy(() -> handler.abandon(instance, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("仅接取人可放弃任务");
    }

    @Test
    void submit_claimedBySameUser_succeeds() {
        TaskInstance instance = TaskInstance.builder()
                .id(1L).status(TaskInstanceStatus.CLAIMED).assigneeId(10L)
                .projectId(2L).itemId(3L).itemTaskNodeId(4L)
                .build();
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(1);
        when(taskSubmissionMapper.insert(any(TaskSubmission.class))).thenReturn(1);

        handler.submit(instance, 10L, "{\"data\":1}");

        verify(taskSubmissionMapper).insert(any(TaskSubmission.class));
        verify(memberSubmitCountHandler).incrementSubmitCount(10L, 2L);
    }

    @Test
    void submit_notClaimed_throws() {
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.PENDING).build();

        assertThatThrownBy(() -> handler.submit(instance, 10L, "{}"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("该任务不可提交");
    }

    @Test
    void reset_completedBySameUser_succeeds() {
        TaskInstance instance = TaskInstance.builder()
                .id(1L).status(TaskInstanceStatus.COMPLETED).assigneeId(10L)
                .projectId(2L)
                .build();
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(1);
        when(taskSubmissionMapper.update(isNull(), any())).thenReturn(1);

        handler.reset(instance, 10L);

        verify(taskSubmissionMapper).update(isNull(), any());
        verify(memberSubmitCountHandler).revertSubmitCount(10L, 2L);
    }

    @Test
    void reset_notCompleted_throws() {
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.CLAIMED).build();

        assertThatThrownBy(() -> handler.reset(instance, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("该任务不可重置");
    }

    @Test
    void reject_selfReject_throws() {
        TaskInstance instance = TaskInstance.builder()
                .id(1L).status(TaskInstanceStatus.COMPLETED).assigneeId(10L)
                .build();

        assertThatThrownBy(() -> handler.reject(instance, 10L, "bad"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("不能打回自己的任务");
    }

    @Test
    void reject_notCompleted_throws() {
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.CLAIMED).assigneeId(10L).build();

        assertThatThrownBy(() -> handler.reject(instance, 20L, "bad"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("该任务不可打回");
    }

    @Test
    void reject_notNextStageExecutor_throws() {
        TaskInstance instance = TaskInstance.builder()
                .id(1L).status(TaskInstanceStatus.COMPLETED).assigneeId(10L)
                .itemTaskNodeId(4L).taskFlowId(5L)
                .build();
        when(itemTaskNodeMapper.selectById(4L)).thenReturn(null);

        assertThatThrownBy(() -> handler.reject(instance, 20L, "bad"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("仅下一阶段执行人可打回该任务");
    }

    @Test
    void reject_nextStageExecutor_succeeds() {
        TaskInstance instance = TaskInstance.builder()
                .id(1L).status(TaskInstanceStatus.COMPLETED).assigneeId(10L)
                .itemTaskNodeId(4L).taskFlowId(5L).projectId(2L)
                .build();
        ItemTaskNode currentNode = ItemTaskNode.builder().id(4L).sort(1).build();
        ItemTaskNode nextNode = ItemTaskNode.builder().id(6L).sort(2).build();
        when(itemTaskNodeMapper.selectById(4L)).thenReturn(currentNode);
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of(nextNode));
        when(taskInstanceMapper.selectCount(any())).thenReturn(1L);
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(1);
        when(taskSubmissionMapper.update(isNull(), any())).thenReturn(1);

        handler.reject(instance, 20L, "需要修改");

        verify(memberSubmitCountHandler).revertSubmitCount(10L, 2L);
    }

    @Test
    void reject_noLaterNodes_throws() {
        TaskInstance instance = TaskInstance.builder()
                .id(1L).status(TaskInstanceStatus.COMPLETED).assigneeId(10L)
                .itemTaskNodeId(4L).taskFlowId(5L)
                .build();
        ItemTaskNode currentNode = ItemTaskNode.builder().id(4L).sort(1).build();
        when(itemTaskNodeMapper.selectById(4L)).thenReturn(currentNode);
        when(itemTaskNodeMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> handler.reject(instance, 20L, "bad"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("仅下一阶段执行人可打回该任务");
    }
}
