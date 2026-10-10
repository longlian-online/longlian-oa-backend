package online.longlian.app.service.orgadmin.impl.tasktemplate;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateNodeCreateParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.common.BaseTaskReferenceService;
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
class TaskTemplateServiceImplTest {

    @Mock
    private TaskTemplateMapper taskTemplateMapper;
    @Mock
    private BaseTaskMapper baseTaskMapper;
    @Mock
    private TaskTemplateNodeMapper taskTemplateNodeMapper;
    @Mock
    private TaskTemplateQueryBuilder taskTemplateQueryBuilder;
    @Mock
    private TaskTemplateAssembler taskTemplateAssembler;
    @Mock
    private TaskTemplateHandler taskTemplateHandler;
    @Mock
    private LockService lockService;

    private TaskTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BaseTask.class);
        service = new TaskTemplateServiceImpl(
                taskTemplateMapper,
                new BaseTaskReferenceService(baseTaskMapper),
                taskTemplateNodeMapper,
                Clock.systemUTC(),
                taskTemplateQueryBuilder,
                taskTemplateAssembler,
                taskTemplateHandler,
                lockService);
    }

    @Test
    void createTaskTemplate_baseTaskMissingAtLock_rejectsWithoutNodeChanges() {
        when(baseTaskMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.createTaskTemplate(TaskTemplateCreateParamsBO.builder()
                .orgId(1L)
                .creatorId(3L)
                .name("组织模板")
                .description("说明")
                .nodes(List.of(node(20L, 1), node(10L, 2)))
                .build()))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
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
