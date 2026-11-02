package online.longlian.app.service.app.impl.tasktemplate;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.app.TaskTemplateOptionsParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.vo.app.TaskTemplateOptionVO;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskTemplateServiceImplTest {

    @Mock private TaskTemplateMapper taskTemplateMapper;
    @Mock private TaskTemplateNodeMapper taskTemplateNodeMapper;
    @Mock private BaseTaskMapper baseTaskMapper;

    private TaskTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskTemplate.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskTemplateNode.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), BaseTask.class);
        service = new TaskTemplateServiceImpl(taskTemplateMapper, taskTemplateNodeMapper, baseTaskMapper);
    }

    @Test
    void listOptions_noTemplates_returnsEmpty() {
        when(taskTemplateMapper.selectList(any())).thenReturn(List.of());

        assertThat(service.listOptions(TaskTemplateOptionsParamsBO.builder().orgId(1L).userId(1L).build())).isEmpty();
    }

    @Test
    void listOptions_missingBaseTask_leavesNameAndStatusEmpty() {
        when(taskTemplateMapper.selectList(any())).thenReturn(List.of(
                TaskTemplate.builder().id(1L).name("模板").status(Status.ENABLED)
                        .scope(TaskTemplateScope.ORGANIZATION).build()), List.of());
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(
                TaskTemplateNode.builder().id(11L).taskTemplateId(1L).baseTaskId(null).sort(1).parallelSort(1).build(),
                TaskTemplateNode.builder().id(12L).taskTemplateId(1L).baseTaskId(22L).sort(2).parallelSort(1).build()));
        when(baseTaskMapper.selectBatchIds(any())).thenReturn(List.of());

        List<TaskTemplateOptionVO> options = service.listOptions(
                TaskTemplateOptionsParamsBO.builder().orgId(1L).userId(1L).build());

        assertThat(options).hasSize(1);
        assertThat(options.get(0).getNodes()).hasSize(2);
        assertThat(options.get(0).getNodes().get(1).getBaseTaskName()).isNull();
        assertThat(options.get(0).getNodes().get(1).getBaseTaskStatus()).isNull();
    }
}
