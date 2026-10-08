package online.longlian.app.service.app.impl.item;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock private ProjectMapper projectMapper;
    @Mock private ItemMapper itemMapper;
    @Mock private ItemTaskFlowMapper itemTaskFlowMapper;
    @Mock private ItemTaskNodeMapper itemTaskNodeMapper;
    @Mock private TaskTemplateMapper taskTemplateMapper;
    @Mock private TaskTemplateNodeMapper taskTemplateNodeMapper;
    @Mock private BaseTaskMapper baseTaskMapper;
    @Mock private TaskInstanceMapper taskInstanceMapper;
    @Mock private ItemQueryBuilder itemQueryBuilder;
    @Mock private ItemAssembler itemAssembler;

    private ItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ItemServiceImpl(projectMapper, itemMapper, itemTaskFlowMapper, itemTaskNodeMapper,
                taskTemplateMapper, taskTemplateNodeMapper, baseTaskMapper, taskInstanceMapper,
                Clock.systemUTC(), itemQueryBuilder, itemAssembler);
    }

    @Test
    void listProjectItems_disabledProject_throwsNotFound() {
        when(projectMapper.selectById(1L)).thenReturn(disabledProject());
        ItemListParamsBO params = ItemListParamsBO.builder().projectId(1L).orgId(1L).build();

        assertThatThrownBy(() -> service.listProjectItems(params)).isInstanceOf(AppException.class);
        verify(itemMapper, never()).selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createProjectItem_disabledProject_throwsNotFound() {
        when(projectMapper.selectById(1L)).thenReturn(disabledProject());
        ItemCreateParamsBO params = ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build();

        assertThatThrownBy(() -> service.createProjectItem(params)).isInstanceOf(AppException.class);
        verify(taskTemplateMapper, never()).selectById(1L);
    }

    @Test
    void createProjectItem_createsEveryInstancePending() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskTemplate.class);
        when(projectMapper.selectById(1L)).thenReturn(enabledProject());
        when(taskTemplateMapper.selectById(1L)).thenReturn(
                TaskTemplate.builder().id(1L).name("模板").status(Status.ENABLED).build());
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(
                TaskTemplateNode.builder().id(11L).baseTaskId(21L).sort(1).parallelSort(1).build(),
                TaskTemplateNode.builder().id(12L).baseTaskId(22L).sort(2).parallelSort(1).build()));
        when(baseTaskMapper.selectBatchIds(any())).thenReturn(List.of());

        service.createProjectItem(ItemCreateParamsBO.builder()
                .projectId(1L).orgId(1L).creatorId(1L).taskTemplateId(1L).title("项目").build());

        ArgumentCaptor<TaskInstance> captor = ArgumentCaptor.forClass(TaskInstance.class);
        verify(taskInstanceMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(instance -> {
            assertThat(instance.getStatus()).isEqualTo(TaskInstanceStatus.PENDING);
            assertThat(instance.getAssigneeId()).isNull();
        });
    }

    private Project enabledProject() {
        return Project.builder().id(1L).orgId(1L).creatorId(1L).resourceStatus(Status.ENABLED).build();
    }

    private Project disabledProject() {
        return Project.builder().id(1L).orgId(1L).creatorId(1L).resourceStatus(Status.DISABLED).build();
    }
}
