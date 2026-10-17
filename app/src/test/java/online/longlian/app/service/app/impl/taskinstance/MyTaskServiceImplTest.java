package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.MyTaskParamsBO;
import online.longlian.app.pojo.bo.app.MyTaskRowBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.vo.app.MyTaskVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyTaskServiceImplTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;

    private MyTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MyTaskServiceImpl(taskInstanceMapper);
    }

    private static MyTaskRowBO row(long instanceId, long itemId, String nodeName, int sort, int minute) {
        MyTaskRowBO row = new MyTaskRowBO();
        row.setInstanceId(instanceId);
        row.setItemId(itemId);
        row.setProjectId(100L);
        row.setItemTitle("第" + itemId + "话");
        row.setProjectName("企划");
        row.setNodeName(nodeName);
        row.setStageSort(sort);
        row.setClaimedAt(LocalDateTime.of(2026, 10, 14, 10, minute));
        return row;
    }

    private static ItemStageNodeStatusBO node(long itemId, long id, String name, int sort, boolean completed) {
        return new ItemStageNodeStatusBO(itemId, id, name, sort, 1, completed);
    }

    private MyTaskParamsBO params(MyTaskBucket bucket, int pageNum, int pageSize) {
        return MyTaskParamsBO.builder().userId(7L).orgId(9L).bucket(bucket)
                .sortDirection(SortDirection.ASC).page(new PageParamsBO(pageNum, pageSize)).build();
    }

    private void givenTwoItems() {
        // item 1: 线稿已完成 → 上色可提交；item 2: 线稿未完成 → 嵌字等「上色」（最近未完成前置）
        when(taskInstanceMapper.selectMyClaimedTasks(7L, 9L, false)).thenReturn(List.of(
                row(11, 1, "上色", 2, 1), row(21, 2, "嵌字", 3, 2), row(12, 1, "线稿复核", 1, 3)));
        when(taskInstanceMapper.selectItemStageNodeStatuses(anyList())).thenReturn(List.of(
                node(1, 1, "线稿复核", 1, true), node(1, 2, "上色", 2, false),
                node(2, 3, "线稿", 1, false), node(2, 4, "上色", 2, false), node(2, 5, "嵌字", 3, false)));
    }

    @Test
    void submittableBucket_keepsUnlockedInClaimOrder() {
        givenTwoItems();

        PageResultVO<MyTaskVO> result = service.listMyTasks(params(MyTaskBucket.SUBMITTABLE, 1, 5));

        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getList()).extracting(MyTaskVO::getInstanceId).containsExactly(11L, 12L);
        assertThat(result.getList()).allMatch(vo -> vo.getWaitingStageName() == null);
    }

    @Test
    void waitingBucket_carriesWaitingStageName() {
        givenTwoItems();

        PageResultVO<MyTaskVO> result = service.listMyTasks(params(MyTaskBucket.WAITING, 1, 5));

        assertThat(result.getTotal()).isEqualTo(1L);
        MyTaskVO vo = result.getList().get(0);
        assertThat(vo.getInstanceId()).isEqualTo(21L);
        assertThat(vo.getWaitingStageName()).isEqualTo("上色");
        assertThat(vo.getItemId()).isEqualTo(2L);
        assertThat(vo.getProjectId()).isEqualTo(100L);
        assertThat(vo.getStageSort()).isEqualTo(3);
    }

    @Test
    void predecessorCompleted_movesToSubmittable() {
        when(taskInstanceMapper.selectMyClaimedTasks(7L, 9L, false)).thenReturn(List.of(row(21, 2, "嵌字", 2, 1)));
        when(taskInstanceMapper.selectItemStageNodeStatuses(anyList())).thenReturn(List.of(
                node(2, 3, "线稿", 1, true), node(2, 5, "嵌字", 2, false)));

        assertThat(service.listMyTasks(params(MyTaskBucket.WAITING, 1, 5)).getTotal()).isZero();
        assertThat(service.listMyTasks(params(MyTaskBucket.SUBMITTABLE, 1, 5)).getList())
                .extracting(MyTaskVO::getInstanceId).containsExactly(21L);
    }

    @Test
    void pagesWithinBucket() {
        givenTwoItems();

        PageResultVO<MyTaskVO> page2 = service.listMyTasks(params(MyTaskBucket.SUBMITTABLE, 2, 1));
        PageResultVO<MyTaskVO> page3 = service.listMyTasks(params(MyTaskBucket.SUBMITTABLE, 3, 1));

        assertThat(page2.getTotal()).isEqualTo(2L);
        assertThat(page2.getList()).extracting(MyTaskVO::getInstanceId).containsExactly(12L);
        assertThat(page3.getList()).isEmpty();
    }

    @Test
    void descSortDirection_passedToMapper() {
        when(taskInstanceMapper.selectMyClaimedTasks(7L, 9L, true)).thenReturn(List.of());

        PageResultVO<MyTaskVO> result = service.listMyTasks(MyTaskParamsBO.builder().userId(7L).orgId(9L)
                .bucket(MyTaskBucket.SUBMITTABLE).sortDirection(SortDirection.DESC)
                .page(new PageParamsBO(1, 5)).build());

        assertThat(result.getTotal()).isZero();
        verify(taskInstanceMapper, never()).selectItemStageNodeStatuses(anyList());
    }

    @Test
    void waitingPredecessor_exposesSerialAndParallelSlot() {
        MyTaskRowBO claimed = row(21, 2, "校对", 4, 1);
        claimed.setNodeId(5L);
        when(taskInstanceMapper.selectMyClaimedTasks(7L, 9L, false)).thenReturn(List.of(claimed));
        when(taskInstanceMapper.selectItemStageNodeStatuses(anyList())).thenReturn(List.of(
                new ItemStageNodeStatusBO(2L, 1L, "校对", 1, 1, false),
                new ItemStageNodeStatusBO(2L, 2L, "校对", 2, 1, false),
                new ItemStageNodeStatusBO(2L, 3L, "翻译", 3, 1, false),
                new ItemStageNodeStatusBO(2L, 4L, "翻译", 3, 2, false),
                new ItemStageNodeStatusBO(2L, 5L, "校对", 4, 1, false)));

        MyTaskVO vo = service.listMyTasks(params(MyTaskBucket.WAITING, 1, 5)).getList().getFirst();

        assertThat(vo.getSlotIndex()).isEqualTo(1);
        assertThat(vo.getSlotTotal()).isEqualTo(1);
        assertThat(vo.getSerialStep()).isEqualTo(3);
        assertThat(vo.getWaitingStageName()).isEqualTo("翻译");
        assertThat(vo.getWaitingSlotIndex()).isEqualTo(1);
        assertThat(vo.getWaitingSlotTotal()).isEqualTo(2);
        assertThat(vo.getWaitingSerialStep()).isNull();
    }
}
