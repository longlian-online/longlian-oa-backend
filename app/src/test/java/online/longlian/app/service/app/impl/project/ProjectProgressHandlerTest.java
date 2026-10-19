package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.ProjectProgressBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.pojo.entity.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
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
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ProjectWorkshop.class);
        handler = new ProjectProgressHandler(
                itemMapper, taskInstanceMapper, projectWorkshopMapper,
                Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"), ZoneOffset.UTC));
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

    /** 软删除历史不占用 active 唯一键，实际插入新行才报告新增。 */
    @Test
    void shouldReturnTrueOnlyAfterInsertingActiveMembership() {
        when(projectWorkshopMapper.insert(any(ProjectWorkshop.class))).thenReturn(1);

        assertThat(handler.addToWorkshop(workshopParams()).isAdded()).isTrue();

        ArgumentCaptor<ProjectWorkshop> inserted = ArgumentCaptor.forClass(ProjectWorkshop.class);
        verify(projectWorkshopMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getProjectId()).isEqualTo(1L);
        assertThat(inserted.getValue().getUserId()).isEqualTo(2L);
        assertThat(inserted.getValue().getDeletedAt()).isNull();
        assertThat(inserted.getValue().getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 19, 0, 0));
        verify(projectWorkshopMapper).selectOne(any());
    }

    /** 唯一键碰撞只能在当前读确认目标有效关系后视为已加入，不依赖数据库异常文案。 */
    @Test
    void shouldReturnFalseAfterDuplicateConfirmsActiveMembership() {
        doThrow(new DuplicateKeyException("duplicate"))
                .when(projectWorkshopMapper).insert(any(ProjectWorkshop.class));
        when(projectWorkshopMapper.selectOne(any())).thenReturn(null,
                ProjectWorkshop.builder().id(3L).projectId(1L).userId(2L).build());

        assertThat(handler.addToWorkshop(workshopParams()).isAdded()).isFalse();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper<ProjectWorkshop>> query =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(projectWorkshopMapper, times(2)).selectOne(query.capture());
        var currentRead = query.getAllValues().get(1);
        assertThat(currentRead.getSqlSegment()).contains("project_id", "user_id")
                .endsWith("LOCK IN SHARE MODE");
        assertThat(currentRead.getSqlSegment()).doesNotContain("FOR UPDATE");
    }

    /** 已加入的常见路径只确认已知主键仍有效，不分配新关联或制造唯一键异常。 */
    @Test
    void shouldSkipInsertWhenKnownMembershipIsStillActive() {
        ProjectWorkshop active = ProjectWorkshop.builder().id(3L).projectId(1L).userId(2L).build();
        when(projectWorkshopMapper.selectOne(any())).thenReturn(active);

        assertThat(handler.addToWorkshop(workshopParams()).isAdded()).isFalse();
        verify(projectWorkshopMapper, never()).insert(any(ProjectWorkshop.class));
        verify(projectWorkshopMapper, times(2)).selectOne(any());
    }

    /** 旧快照中的关联已经退出时，当前读不应误报已加入，必须真正插入新行。 */
    @Test
    void shouldInsertWhenSnapshotMembershipIsNoLongerActive() {
        when(projectWorkshopMapper.selectOne(any())).thenReturn(
                ProjectWorkshop.builder().id(3L).projectId(1L).userId(2L).build(), (ProjectWorkshop) null);
        when(projectWorkshopMapper.insert(any(ProjectWorkshop.class))).thenReturn(1);

        assertThat(handler.addToWorkshop(workshopParams()).isAdded()).isTrue();
        verify(projectWorkshopMapper).insert(any(ProjectWorkshop.class));
    }

    /** 主键碰撞到其他关联且目标未加入时，不能吞掉原异常。 */
    @Test
    void shouldPropagateDuplicateWhenTargetMembershipDoesNotExist() {
        DuplicateKeyException failure = new DuplicateKeyException("PRIMARY collision");
        doThrow(failure).when(projectWorkshopMapper).insert(any(ProjectWorkshop.class));
        when(projectWorkshopMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> handler.addToWorkshop(workshopParams())).isSameAs(failure);
    }

    /** 非唯一键写入故障必须直接失败，不能误判为已加入。 */
    @Test
    void shouldPropagateOtherMembershipWriteFailures() {
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("unavailable");
        doThrow(failure).when(projectWorkshopMapper).insert(any(ProjectWorkshop.class));

        assertThatThrownBy(() -> handler.addToWorkshop(workshopParams())).isSameAs(failure);
        verify(projectWorkshopMapper).selectOne(any());
    }

    private ProjectWorkshopAddParamsBO workshopParams() {
        return ProjectWorkshopAddParamsBO.builder().projectId(1L).userId(2L).orgId(1L).build();
    }
}
