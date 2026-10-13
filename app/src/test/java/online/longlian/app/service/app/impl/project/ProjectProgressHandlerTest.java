package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.ProjectProgressBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectProgressHandlerTest {

    @Mock
    private ItemMapper itemMapper;
    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private ProjectWorkshopMapper projectWorkshopMapper;

    private ProjectProgressHandler handler;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Item.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), TaskInstance.class);
        handler = new ProjectProgressHandler(
                itemMapper, taskInstanceMapper, projectWorkshopMapper, Clock.systemUTC());
    }

    @Test
    void computeProgress_noItems_returnsZeroWithoutCountingTasks() {
        when(itemMapper.selectObjs(any())).thenReturn(List.of());

        ProjectProgressBO progress = handler.computeProgress(2108182494998228993L);

        assertThat(progress.getProgressPercent()).isZero();
        assertThat(progress.getClaimedTaskCount()).isZero();
        assertThat(progress.getPendingTaskCount()).isZero();
        verify(taskInstanceMapper, never()).selectCount(any());
    }

    @Test
    void computeProgress_usesCompletedTaskInstancesOverTotal() {
        when(itemMapper.selectObjs(any())).thenReturn(List.of(1L, 2L));
        // total, completed, claimed, pending
        when(taskInstanceMapper.selectCount(any())).thenReturn(3L, 1L, 1L, 1L);

        ProjectProgressBO progress = handler.computeProgress(2108182494998228993L);

        assertThat(progress.getProgressPercent()).isEqualTo(33);
        assertThat(progress.getClaimedTaskCount()).isEqualTo(1);
        assertThat(progress.getPendingTaskCount()).isEqualTo(1);
        verify(itemMapper, never()).selectCount(any());
    }

    @Test
    void computeProgress_itemsWithoutTaskInstances_returnsZero() {
        when(itemMapper.selectObjs(any())).thenReturn(List.of(1L));
        when(taskInstanceMapper.selectCount(any())).thenReturn(0L, 0L, 0L, 0L);

        assertThat(handler.computeProgress(1L).getProgressPercent()).isZero();
    }

    @Test
    void computeProgress_allTaskInstancesCompleted_returns100() {
        when(itemMapper.selectObjs(any())).thenReturn(List.of(1L));
        when(taskInstanceMapper.selectCount(any())).thenReturn(5L, 5L, 0L, 0L);

        ProjectProgressBO progress = handler.computeProgress(1L);

        assertThat(progress.getProgressPercent()).isEqualTo(100);
        assertThat(progress.getClaimedTaskCount()).isZero();
        assertThat(progress.getPendingTaskCount()).isZero();
    }
}
