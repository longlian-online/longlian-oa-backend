package online.longlian.app.service.orgadmin.impl.tasktemplate;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateNodeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateUpdateParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskTemplateHandlerTest {

    @Mock
    private TaskTemplateMapper taskTemplateMapper;
    @Mock
    private BaseTaskMapper baseTaskMapper;
    @Mock
    private TaskTemplateNodeMapper taskTemplateNodeMapper;

    private TaskTemplateHandler handler;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), TaskTemplate.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BaseTask.class);
        handler = new TaskTemplateHandler(
                taskTemplateMapper, baseTaskMapper, taskTemplateNodeMapper, Clock.systemUTC());
    }

    @Test
    void updateTaskTemplate_baseTaskMissingAtLock_rejectsWithoutNodeChanges() {
        when(taskTemplateMapper.selectById(8L)).thenReturn(TaskTemplate.builder()
                .id(8L)
                .orgId(1L)
                .status(Status.ENABLED)
                .build());
        when(baseTaskMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> handler.updateTaskTemplate(TaskTemplateUpdateParamsBO.builder()
                .templateId(8L)
                .orgId(1L)
                .name("修订模板")
                .description("说明")
                .nodes(List.of(node(20L, 1), node(10L, 2)))
                .build()))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(ex.getMessage()).isEqualTo("原子任务不存在或已禁用");
                    assertThat(ex.getMsg()).isEqualTo("参数错误,原子任务不存在或已禁用");
                });

        verifyNoInteractions(taskTemplateNodeMapper);
    }

    private static TaskTemplateNodeCreateParamsBO node(Long baseTaskId, int sort) {
        return TaskTemplateNodeCreateParamsBO.builder()
                .baseTaskId(baseTaskId)
                .sort(sort)
                .parallelSort(1)
                .build();
    }
}
