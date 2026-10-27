package online.longlian.app.service.app.impl.notification;

import online.longlian.app.mapper.*;
import online.longlian.app.pojo.bo.app.*;
import online.longlian.app.pojo.entity.*;
import online.longlian.common.enumeration.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSourceResolverTest {
    @Mock TaskInstanceMapper tasks;
    @Mock ItemMapper items;
    @Mock ProjectMapper projects;
    @Mock GroupApplicationMapper applications;
    @Mock DeletionRequestMapper deletions;
    @Mock UserMapper users;
    NotificationSourceResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new NotificationSourceResolver(tasks, items, projects, applications, deletions, users);
    }

    @Test
    void shouldNotReadSourcesForEmptyInbox() {
        assertThat(resolver.resolve(new NotificationSourceResolveParamsBO(1L, List.of()))).isEmpty();
        verifyNoInteractions(tasks, items, projects);
    }

    @Test
    void shouldResolveSameNumericIdByKindAndRetainHistoricalTaskAfterReassignment() {
        when(tasks.selectBatchIds(anyCollection())).thenReturn(List.of(TaskInstance.builder().id(7L)
                .itemId(8L).projectId(9L).itemTaskNodeId(11L).status(TaskInstanceStatus.COMPLETED).assigneeId(99L).build()));
        when(items.selectBatchIds(anyCollection())).thenReturn(List.of(
                Item.builder().id(7L).projectId(10L).title("完成项目").build(),
                Item.builder().id(8L).projectId(9L).title("任务项目").build()));
        when(projects.selectList(any())).thenReturn(List.of(
                Project.builder().id(9L).orgId(1L).title("任务企划").status(ProjectStatus.ARCHIVED)
                        .thumbnailFileId(20L).coverFileId(21L).build(),
                Project.builder().id(10L).orgId(1L).title("完成企划").build()));
        when(tasks.selectItemStageNodeStatuses(anyList())).thenReturn(List.of(
                new ItemStageNodeStatusBO(8L, 11L, "翻译", 2, 1, true),
                new ItemStageNodeStatusBO(8L, 12L, "翻译", 1, 1, true)));
        Map<Long, NotificationResolvedSourceBO> result = resolver.resolve(new NotificationSourceResolveParamsBO(1L, List.of(
                new NotificationSourceRefBO(1L, InboxNotificationKind.TASK_READY, 7L),
                new NotificationSourceRefBO(2L, InboxNotificationKind.ITEM_COMPLETED, 7L),
                new NotificationSourceRefBO(3L, InboxNotificationKind.TASK_ASSIGNED, 7L))));
        assertThat(result.get(1L).getSource().getItemId()).isEqualTo(8L);
        assertThat(result.get(1L).getSource().getProjectId()).isEqualTo(9L);
        assertThat(result.get(1L).getSource().getTaskSlot().getSerialStep()).isEqualTo(2);
        assertThat(result.get(2L).getSource().getItemId()).isEqualTo(7L);
        assertThat(result.get(2L).getSource().getProjectId()).isEqualTo(10L);
        assertThat(result.get(2L).getSource().getTaskSlot()).isNull();
        assertThat(result.get(3L).getSource()).isEqualTo(result.get(1L).getSource());
        assertThat(result.get(1L).getImageResourceIds()).containsExactly(20L, 21L);
        verify(tasks).selectBatchIds(List.of(7L));
        verify(items).selectBatchIds(anyCollection());
        verify(projects).selectList(any());
    }

    @Test
    void shouldResolveJoinAndDeletionSourcesWithoutReadingProjects() {
        when(applications.selectBatchIds(anyCollection())).thenReturn(List.of(
                GroupApplication.builder().id(4L).orgId(1L).nickname("  小周  ").username("zhou").build(),
                GroupApplication.builder().id(5L).orgId(2L).nickname("别人").build()));
        when(deletions.selectBatchIds(anyCollection())).thenReturn(List.of(
                DeletionRequest.builder().id(8L).orgId(1L).requesterId(3L).targetType(DeletionTargetType.ITEM)
                        .targetTitle(" 第一章 ").build(),
                DeletionRequest.builder().id(9L).orgId(1L).requesterId(99L).targetType(DeletionTargetType.PROJECT)
                        .targetTitle("失踪").build()));
        when(users.selectBatchIds(anyCollection())).thenReturn(List.of(
                User.builder().id(3L).nickname("").username("申请人").build()));

        Map<Long, NotificationResolvedSourceBO> result = resolver.resolve(new NotificationSourceResolveParamsBO(1L, List.of(
                new NotificationSourceRefBO(1L, InboxNotificationKind.JOIN_APPLICATION, 4L),
                new NotificationSourceRefBO(2L, InboxNotificationKind.JOIN_APPLICATION, 5L),
                new NotificationSourceRefBO(3L, InboxNotificationKind.DELETION_REQUEST, 8L),
                new NotificationSourceRefBO(4L, InboxNotificationKind.DELETION_REQUEST, 9L))));

        assertThat(result.get(1L).getSource().getApplicantName()).isEqualTo("小周");
        assertThat(result.get(1L).getImageResourceIds()).isEmpty();
        assertThat(result.get(2L).getSource()).isNull();
        assertThat(result.get(3L).getSource().getRequesterName()).isEqualTo("申请人");
        assertThat(result.get(3L).getSource().getTargetType()).isEqualTo("ITEM");
        assertThat(result.get(3L).getSource().getTargetTitle()).isEqualTo("第一章");
        assertThat(result.get(4L).getSource()).isNull();
        verifyNoInteractions(tasks, items, projects);
    }

    @Test
    void shouldInvalidateOnlyMissingOrInconsistentRelationships() {
        when(tasks.selectBatchIds(anyCollection())).thenReturn(List.of(
                TaskInstance.builder().id(1L).itemId(7L).projectId(9L).itemTaskNodeId(101L).build(),
                TaskInstance.builder().id(2L).itemId(7L).projectId(99L).itemTaskNodeId(101L).build(),
                TaskInstance.builder().id(3L).itemId(7L).projectId(9L).itemTaskNodeId(999L).build()));
        when(items.selectBatchIds(anyCollection())).thenReturn(List.of(Item.builder().id(7L).projectId(9L).title("项目").build()));
        when(projects.selectList(any())).thenReturn(List.of(Project.builder().id(9L).title("企划").build()));
        when(tasks.selectItemStageNodeStatuses(anyList())).thenReturn(List.of(
                new ItemStageNodeStatusBO(7L,101L,"翻译",1,1,false)));
        var result = resolver.resolve(new NotificationSourceResolveParamsBO(1L,List.of(
                new NotificationSourceRefBO(1L,InboxNotificationKind.TASK_READY,1L),
                new NotificationSourceRefBO(2L,InboxNotificationKind.TASK_READY,2L),
                new NotificationSourceRefBO(3L,InboxNotificationKind.TASK_READY,3L),
                new NotificationSourceRefBO(4L,InboxNotificationKind.ITEM_COMPLETED,999L))));
        assertThat(result.get(1L).getSource().getItemId()).isEqualTo(7L);
        for (long id : List.of(2L,3L,4L)) {
            assertThat(result.get(id).getSource()).isNull();
            assertThat(result.get(id).getImageResourceIds()).isEmpty();
        }
    }

    @Test
    void shouldPropagateDatabaseFailureRatherThanMaskSourceAsMissing() {
        when(items.selectBatchIds(anyCollection())).thenThrow(new IllegalStateException("database offline"));
        assertThatThrownBy(() -> resolver.resolve(new NotificationSourceResolveParamsBO(1L,List.of(
                new NotificationSourceRefBO(1L,InboxNotificationKind.ITEM_COMPLETED,1L)))))
                .isInstanceOf(IllegalStateException.class).hasMessage("database offline");
    }
}
