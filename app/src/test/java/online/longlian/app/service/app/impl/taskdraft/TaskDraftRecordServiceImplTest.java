package online.longlian.app.service.app.impl.taskdraft;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskDraftMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.TaskDraftConsumeParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftDiscardParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordSaveParamsBO;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskDraft;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.TaskDraftStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDraftRecordServiceImplTest {

    @Mock private TaskDraftMapper mapper;
    @Mock private ResourceService resources;
    @Mock private TaskInstanceMapper instances;
    @Mock private ItemTaskNodeMapper nodes;
    @Mock private TaskSubmissionMapper submissions;
    private TaskDraftRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskDraft.class);
        service = new TaskDraftRecordServiceImpl(resources,
                Clock.fixed(Instant.parse("2026-11-06T00:00:00Z"), ZoneOffset.UTC),
                new TaskFormService(), instances, nodes, submissions);
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
        verifyNoInteractions(instances, resources, submissions);
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
        when(instances.selectById(1L)).thenReturn(TaskInstance.builder().id(1L).itemTaskNodeId(4L).build());
        when(nodes.selectById(4L)).thenReturn(ItemTaskNode.builder().metaSchema("[]").build());
        service.saveTaskDraftRecord(saveParams("10", "{}"));
        assertThat(previous.getMetadata()).isEqualTo("{}");
        ArgumentCaptor<TaskDraft> inserted = ArgumentCaptor.forClass(TaskDraft.class);
        verify(mapper).insert(inserted.capture());
        assertThat(inserted.getValue()).isNotSameAs(previous);
        assertThat(inserted.getValue().getStatus()).isEqualTo(TaskDraftStatus.ACTIVE);
        verifyNoInteractions(resources, submissions);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldKeepFormalHistoryFilesWhenDiscardingDraft(boolean unreadable) {
        TaskDraft draft = TaskDraft.builder().id(10L).taskInstanceId(1L).orgId(2L).ownerId(3L)
                .metadata("{\"file\":{\"fileId\":\"99\"}}").status(TaskDraftStatus.ACTIVE).build();
        when(mapper.selectList(any())).thenReturn(List.of(draft));
        when(instances.selectById(1L)).thenReturn(TaskInstance.builder().id(1L).itemTaskNodeId(4L).build());
        when(nodes.selectById(4L)).thenReturn(ItemTaskNode.builder()
                .metaSchema("[{\"key\":\"file\",\"label\":\"附件\",\"type\":\"file\",\"required\":false,\"options\":[]}]").build());
        when(submissions.selectList(any())).thenReturn(List.of(TaskSubmission.builder()
                .metadata(unreadable ? "invalid" : draft.getMetadata()).build()));
        service.discardTaskDrafts(new TaskDraftDiscardParamsBO(List.of(1L), 2L));
        verifyNoInteractions(resources);
        assertThat(draft.getMetadata()).contains("99");
    }

    @Test
    void shouldRequireActiveSnapshotWhenConsuming() {
        assertThatThrownBy(() -> service.consumeTaskDraftRecord(new TaskDraftConsumeParamsBO(10L, 20L)))
                .isInstanceOf(AppException.class);
        when(mapper.update(isNull(), any())).thenReturn(1);
        service.consumeTaskDraftRecord(new TaskDraftConsumeParamsBO(10L, 20L));
        verifyNoInteractions(resources, instances, submissions);
    }
}
