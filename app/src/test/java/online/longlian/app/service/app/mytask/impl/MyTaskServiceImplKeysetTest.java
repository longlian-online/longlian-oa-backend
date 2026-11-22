package online.longlian.app.service.app.mytask.impl;

import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetItemBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetParamsBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetResultBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskRowBO;
import online.longlian.app.pojo.bo.app.taskinstance.ItemStageNodeStatusBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 我的待办键集分页单元测试：分组过滤、(claimed_at, id) 游标、hasMore 与状态/修订号口径。
 */
class MyTaskServiceImplKeysetTest {

    private static final Long USER_ID = 9L;
    private static final Long ORG_ID = 8L;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 9, 0, 0);

    private TaskInstanceMapper taskInstanceMapper;
    private MyTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        taskInstanceMapper = mock(TaskInstanceMapper.class);
        service = new MyTaskServiceImpl(taskInstanceMapper);
        when(taskInstanceMapper.selectItemStageNodeStatuses(anyList())).thenReturn(List.of());
    }

    private MyTaskRowBO row(long instanceId, LocalDateTime claimedAt, long revision) {
        MyTaskRowBO row = new MyTaskRowBO();
        row.setInstanceId(instanceId);
        row.setItemId(42000L + instanceId - 43000L);
        row.setProjectId(41000L);
        row.setItemTitle("项目" + instanceId);
        row.setProjectName("企划");
        row.setNodeId(43000L + instanceId);
        row.setNodeName("节点" + instanceId);
        row.setStageSort(0);
        row.setClaimedAt(claimedAt);
        row.setRevision(revision);
        return row;
    }

    private MyTaskKeysetParamsBO params(Integer limit, MyTaskBucket bucket,
                                        LocalDateTime afterClaimedAt, Long afterInstanceId) {
        return new MyTaskKeysetParamsBO(USER_ID, ORG_ID, bucket, afterClaimedAt, afterInstanceId,
                limit == null ? 50 : limit);
    }

    @Test
    void shouldPageClaimedTasksWithHasMoreAndCursorKey() {
        when(taskInstanceMapper.selectMyClaimedTasks(eq(USER_ID), eq(ORG_ID), eq(false))).thenReturn(List.of(
                row(43001L, BASE, 3L), row(43002L, BASE.plusHours(1), 4L), row(43003L, BASE.plusHours(2), 5L)));

        MyTaskKeysetResultBO page1 = service.listMyTasksKeyset(params(2, null, null, null));
        assertThat(page1.list()).extracting(MyTaskKeysetItemBO::instanceId).containsExactly(43001L, 43002L);
        assertThat(page1.hasMore()).isTrue();
        assertThat(page1.lastClaimedAt()).isEqualTo(BASE.plusHours(1));
        assertThat(page1.lastInstanceId()).isEqualTo(43002L);

        MyTaskKeysetResultBO page2 = service.listMyTasksKeyset(
                params(2, null, page1.lastClaimedAt(), page1.lastInstanceId()));
        assertThat(page2.list()).extracting(MyTaskKeysetItemBO::instanceId).containsExactly(43003L);
        assertThat(page2.hasMore()).isFalse();
        // 键集结果始终携带本页最后一行的键；是否编码游标由 hasMore 决定（开放层只看 hasMore）
        assertThat(page2.lastClaimedAt()).isEqualTo(BASE.plusHours(2));
        assertThat(page2.lastInstanceId()).isEqualTo(43003L);
    }

    @Test
    void shouldFilterByBucketWithoutLosingCursorOrder() {
        // 43001 等前置（阶段 1 且阶段 0 未完成），43002 可提交
        MyTaskRowBO waiting = row(43001L, BASE, 3L);
        waiting.setStageSort(1);
        MyTaskRowBO submittable = row(43002L, BASE.plusHours(1), 4L);
        when(taskInstanceMapper.selectMyClaimedTasks(eq(USER_ID), eq(ORG_ID), eq(false)))
                .thenReturn(List.of(waiting, submittable));
        when(taskInstanceMapper.selectItemStageNodeStatuses(anyList())).thenReturn(List.of(
                new ItemStageNodeStatusBO(42001L, 43101L, "前置节点", 0, 0, false)));

        MyTaskKeysetResultBO waitingPage = service.listMyTasksKeyset(params(50, MyTaskBucket.WAITING, null, null));
        assertThat(waitingPage.list()).hasSize(1);
        assertThat(waitingPage.list().get(0).bucket()).isEqualTo(MyTaskBucket.WAITING);
        assertThat(waitingPage.list().get(0).instanceId()).isEqualTo(43001L);

        MyTaskKeysetResultBO submittablePage = service.listMyTasksKeyset(
                params(50, MyTaskBucket.SUBMITTABLE, null, null));
        assertThat(submittablePage.list()).extracting(MyTaskKeysetItemBO::instanceId).containsExactly(43002L);
        assertThat(submittablePage.list().get(0).bucket()).isEqualTo(MyTaskBucket.SUBMITTABLE);
    }

    @Test
    void shouldExposeClaimedStatusAndRevisionForOptimisticLock() {
        when(taskInstanceMapper.selectMyClaimedTasks(eq(USER_ID), eq(ORG_ID), eq(false)))
                .thenReturn(List.of(row(43001L, BASE, 7L)));

        MyTaskKeysetResultBO page = service.listMyTasksKeyset(params(50, null, null, null));
        MyTaskKeysetItemBO item = page.list().get(0);
        assertThat(item.taskStatus().name()).isEqualTo("CLAIMED");
        assertThat(item.revision()).isEqualTo(7L);
        assertThat(item.claimedAt()).isEqualTo(BASE);
        assertThat(item.itemTitle()).isEqualTo("项目43001");
        assertThat(item.projectName()).isEqualTo("企划");
    }

    @Test
    void shouldReturnEmptyPageWhenNoClaimedTasks() {
        when(taskInstanceMapper.selectMyClaimedTasks(eq(USER_ID), eq(ORG_ID), eq(false)))
                .thenReturn(List.of());

        MyTaskKeysetResultBO page = service.listMyTasksKeyset(params(50, null, null, null));
        assertThat(page.list()).isEmpty();
        assertThat(page.hasMore()).isFalse();
        assertThat(page.lastInstanceId()).isNull();
    }
}
