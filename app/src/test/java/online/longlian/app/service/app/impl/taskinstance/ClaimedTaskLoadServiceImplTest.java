package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.pojo.bo.app.ProjectClaimedTaskParamsBO;
import online.longlian.app.pojo.entity.TaskInstance;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadParamsBO;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadResultBO;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadRowBO;
import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimedTaskLoadServiceImplTest {

    @Mock
    private TaskInstanceMapper taskInstanceMapper;

    private ClaimedTaskLoadServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskInstance.class);
        service = new ClaimedTaskLoadServiceImpl(taskInstanceMapper);
    }

    /** 存在待处理责任时拒绝退出，无需加载全部待办或计算阶段。 */
    @Test
    void shouldFindClaimedResponsibilityInProject() {
        when(taskInstanceMapper.selectOne(any())).thenReturn(TaskInstance.builder().id(1L).build());

        assertThat(service.hasClaimedTasksInProject(ProjectClaimedTaskParamsBO.builder()
                .projectId(10L).assigneeId(2L).build())).isTrue();
        verify(taskInstanceMapper, never()).selectItemStageNodeStatuses(anyList());
    }

    /** 没有未完成责任时允许退出。 */
    @Test
    void shouldFindNoClaimedResponsibilityInProject() {
        assertThat(service.hasClaimedTasksInProject(ProjectClaimedTaskParamsBO.builder()
                .projectId(10L).assigneeId(2L).build())).isFalse();
    }

    @Test
    void emptyAssigneesDoesNotQuery() {
        List<ClaimedTaskLoadResultBO> result = service.listClaimedTasks(ClaimedTaskLoadParamsBO.builder()
                .orgId(1L)
                .assigneeIds(List.of())
                .build());

        assertThat(result).isEmpty();
        verify(taskInstanceMapper, never()).selectClaimedTasksByAssignees(eq(1L), anyList());
    }

    @Test
    void distinguishesSubmittableWaitingAndSlot() {
        when(taskInstanceMapper.selectClaimedTasksByAssignees(9L, List.of(7L))).thenReturn(List.of(
                row(11, 1, 10, "翻译", 1),
                row(21, 2, 20, "校对", 2)));
        when(taskInstanceMapper.selectItemStageNodeStatuses(anyList())).thenReturn(List.of(
                node(1, 10, "翻译", 1, 1, false),
                node(1, 12, "翻译", 1, 2, false),
                node(2, 19, "校对", 1, 1, false),
                node(2, 20, "校对", 2, 1, false)));

        List<ClaimedTaskLoadResultBO> result = service.listClaimedTasks(ClaimedTaskLoadParamsBO.builder()
                .orgId(9L)
                .assigneeIds(List.of(7L))
                .build());

        assertThat(result).extracting(ClaimedTaskLoadResultBO::getInstanceId).containsExactly(11L, 21L);
        ClaimedTaskLoadResultBO parallel = result.get(0);
        assertThat(parallel.getBucket()).isEqualTo(MyTaskBucket.SUBMITTABLE);
        assertThat(parallel.getWaitingStageName()).isNull();
        assertThat(parallel.getSlotIndex()).isEqualTo(1);
        assertThat(parallel.getSlotTotal()).isEqualTo(2);
        assertThat(parallel.getSerialStep()).isNull();
        assertThat(parallel.getClaimedAt()).isEqualTo(CLAIMED_AT);
        ClaimedTaskLoadResultBO waiting = result.get(1);
        assertThat(waiting.getBucket()).isEqualTo(MyTaskBucket.WAITING);
        assertThat(waiting.getWaitingStageName()).isEqualTo("校对");
        assertThat(waiting.getSerialStep()).isEqualTo(2);
        assertThat(waiting.getWaitingSerialStep()).isEqualTo(1);
    }

    @Test
    void noRowsSkipsStageLookup() {
        when(taskInstanceMapper.selectClaimedTasksByAssignees(9L, List.of(7L))).thenReturn(List.of());

        assertThat(service.listClaimedTasks(ClaimedTaskLoadParamsBO.builder()
                .orgId(9L)
                .assigneeIds(List.of(7L))
                .build())).isEmpty();
        verify(taskInstanceMapper, never()).selectItemStageNodeStatuses(anyList());
    }

    private static final LocalDateTime CLAIMED_AT = LocalDateTime.of(2026, 10, 1, 10, 0);

    private static ClaimedTaskLoadRowBO row(long instanceId, long itemId, long nodeId, String name, int sort) {
        ClaimedTaskLoadRowBO row = new ClaimedTaskLoadRowBO();
        row.setInstanceId(instanceId);
        row.setAssigneeId(7L);
        row.setItemId(itemId);
        row.setProjectId(100L);
        row.setItemTitle("第" + itemId + "话");
        row.setProjectName("企划");
        row.setNodeName(name);
        row.setNodeId(nodeId);
        row.setStageSort(sort);
        row.setClaimedAt(CLAIMED_AT);
        return row;
    }

    private static ItemStageNodeStatusBO node(long itemId, long id, String name, int sort, int parallelSort,
                                               boolean completed) {
        return new ItemStageNodeStatusBO(itemId, id, name, sort, parallelSort, completed);
    }
}
