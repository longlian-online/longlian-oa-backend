package online.longlian.app.service.app.impl.taskdraft;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.TaskDraftMapper;
import online.longlian.app.pojo.bo.app.TaskDraftConsumeParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftDiscardParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordSaveParamsBO;
import online.longlian.app.pojo.entity.TaskDraft;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.TaskDraftStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDraftRecordServiceImplTest {

    @Mock private TaskDraftMapper mapper;
    @Mock private ResourceService resources;
    private TaskDraftRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskDraft.class);
        service = new TaskDraftRecordServiceImpl(resources,
                Clock.fixed(Instant.parse("2026-11-06T00:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    private TaskDraftRecordSaveParamsBO saveParams(String previousId, String metadata) {
        return TaskDraftRecordSaveParamsBO.builder().instanceId(1L).orgId(2L).userId(3L)
                .expectedDraftId(previousId).metadata(metadata).build();
    }

    @Test
    void shouldInsertFirstSnapshotWithoutMutatingTask() {
        when(mapper.insert(any(TaskDraft.class))).thenAnswer(call -> {
            call.getArgument(0, TaskDraft.class).setId(11L);
            return 1;
        });
        var result = service.saveTaskDraftRecord(saveParams(null, "{}"));
        assertThat(result.getId()).isEqualTo(11L);
        assertThat(result.getStatus()).isEqualTo(TaskDraftStatus.ACTIVE);
        verifyNoInteractions(resources);
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void shouldRejectStaleIdBeforeReplacingOrInserting() {
        when(mapper.selectOne(any(), eq(true))).thenReturn(TaskDraft.builder().id(10L).status(TaskDraftStatus.ACTIVE).build());
        assertThatThrownBy(() -> service.saveTaskDraftRecord(saveParams("9", "{}")))
                .isInstanceOf(AppException.class).hasMessageContaining("请重新加载");
        verify(mapper, never()).insert(any(TaskDraft.class));
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void shouldKeepOldSnapshotContentWhenReplacing() {
        TaskDraft previous = TaskDraft.builder().id(10L).taskInstanceId(1L).orgId(2L)
                .metadata("{}").status(TaskDraftStatus.ACTIVE).build();
        when(mapper.selectOne(any(), eq(true))).thenReturn(previous);
        service.saveTaskDraftRecord(saveParams("10", "{}"));
        assertThat(previous.getMetadata()).isEqualTo("{}");
        ArgumentCaptor<TaskDraft> inserted = ArgumentCaptor.forClass(TaskDraft.class);
        verify(mapper).insert(inserted.capture());
        assertThat(inserted.getValue()).isNotSameAs(previous);
        assertThat(inserted.getValue().getStatus()).isEqualTo(TaskDraftStatus.ACTIVE);
        verifyNoInteractions(resources);
    }

    @Test
    void shouldDiscardAllUnsubmittedDraftsWithoutChangingSnapshotContent() {
        TaskDraft first = TaskDraft.builder().id(10L).taskInstanceId(1L).orgId(2L)
                .metadata("{}").status(TaskDraftStatus.REPLACED).revision(0L).build();
        TaskDraft second = TaskDraft.builder().id(11L).taskInstanceId(1L).orgId(2L)
                .metadata("{}").status(TaskDraftStatus.ACTIVE).revision(0L).build();
        when(mapper.selectList(any())).thenReturn(List.of(first, second));
        service.discardTaskDrafts(new TaskDraftDiscardParamsBO(List.of(1L), 2L));
        verify(resources).abandonBound(argThat(params -> params.getBizType().equals("task_draft")
                && params.getBizIds().equals(List.of(10L, 11L)) && params.getOrgId().equals(2L)));
        verify(mapper, times(2)).update(isNull(), any());
        assertThat(first.getMetadata()).isEqualTo("{}");
        assertThat(first.getRevision()).isZero();
        assertThat(second.getRevision()).isZero();
    }

    @Test
    void shouldAllowDiscardingTaskWithoutAnyDraft() {
        service.discardTaskDrafts(new TaskDraftDiscardParamsBO(List.of(1L), 2L));
        verifyNoInteractions(resources);
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void shouldAbandonDraftsAndSubmissionsForTheSameInstances() {
        TaskDraft draft = TaskDraft.builder().id(10L).taskInstanceId(1L).orgId(2L)
                .status(TaskDraftStatus.ACTIVE).build();
        when(mapper.selectList(any())).thenReturn(List.of(draft));
        service.abandonTaskInstanceResources(new TaskDraftDiscardParamsBO(List.of(1L), 2L));
        verify(resources).abandonBound(argThat(params -> params.getBizType().equals("task_draft")
                && params.getBizIds().equals(List.of(10L))));
        verify(resources).abandonBound(argThat(params -> params.getBizType().equals("task_submit")
                && params.getBizIds().equals(List.of(1L)) && params.getOrgId().equals(2L)));
    }

    @Test
    void shouldAbandonSubmissionsWhenTheInstanceHasNoDraft() {
        service.abandonTaskInstanceResources(new TaskDraftDiscardParamsBO(List.of(1L), 2L));
        verify(resources).abandonBound(argThat(params -> params.getBizType().equals("task_submit")
                && params.getBizIds().equals(List.of(1L))));
        verify(resources, never()).abandonBound(argThat(params -> params.getBizType().equals("task_draft")));
    }

    @Test
    void shouldSkipAbandonWhenThereIsNoInstance() {
        service.abandonTaskInstanceResources(new TaskDraftDiscardParamsBO(List.of(), 2L));
        verifyNoInteractions(resources, mapper);
    }

    @Test
    void shouldRequireActiveSnapshotWhenConsuming() {
        assertThatThrownBy(() -> service.consumeTaskDraftRecord(new TaskDraftConsumeParamsBO(10L, 20L)))
                .isInstanceOf(AppException.class);
        when(mapper.update(isNull(), any())).thenReturn(1);
        service.consumeTaskDraftRecord(new TaskDraftConsumeParamsBO(10L, 20L));
        verifyNoInteractions(resources);
    }
}
