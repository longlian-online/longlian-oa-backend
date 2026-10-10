package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateNodeCreateParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateUpdateParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.service.common.BaseTaskReferenceService;
import online.longlian.common.enumeration.TaskTemplateScope;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopProjectHandlerTest {

    @Mock
    private ProjectTypeMapper projectTypeMapper;
    @Mock
    private BaseTaskMapper baseTaskMapper;
    @Mock
    private TaskTemplateMapper taskTemplateMapper;
    @Mock
    private TaskTemplateNodeMapper taskTemplateNodeMapper;

    private WorkshopProjectHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WorkshopProjectHandler(
                projectTypeMapper, taskTemplateMapper, new BaseTaskReferenceService(baseTaskMapper), taskTemplateNodeMapper, Clock.systemUTC());
    }

    @Test
    void resolveTypeId_blankProjectType_returnsNoFilter() {
        assertThat(handler.resolveTypeId(1L, "  ")).isNull();

        verify(projectTypeMapper, never()).selectOne(any());
    }

    @Test
    void resolveTypeId_enabledProjectType_returnsTypeId() {
        when(projectTypeMapper.selectOne(any())).thenReturn(ProjectType.builder().id(10L).build());

        assertThat(handler.resolveTypeId(1L, "  漫画  ")).isEqualTo(10L);
    }

    @Test
    void resolveTypeId_unknownOrDisabledProjectType_throwsParameterError() {
        when(projectTypeMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> handler.resolveTypeId(1L, "不存在的类型"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void updateWorkshopTaskTemplate_baseTaskMissingAtLock_rejectsWithoutNodeChanges() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), TaskTemplate.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BaseTask.class);
        when(taskTemplateMapper.selectById(8L)).thenReturn(TaskTemplate.builder()
                .id(8L)
                .scope(TaskTemplateScope.PERSONAL)
                .creatorId(3L)
                .build());
        when(baseTaskMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> handler.updateWorkshopTaskTemplate(WorkshopTaskTemplateUpdateParamsBO.builder()
                .templateId(8L)
                .orgId(1L)
                .userId(3L)
                .name("修订模板")
                .description("说明")
                .nodes(List.of(
                        node(20L, 1),
                        node(10L, 2)))
                .build()))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                });

        verifyNoInteractions(taskTemplateNodeMapper);
    }

    private static WorkshopTaskTemplateNodeCreateParamsBO node(Long baseTaskId, int sort) {
        return WorkshopTaskTemplateNodeCreateParamsBO.builder()
                .baseTaskId(baseTaskId)
                .sort(sort)
                .parallelSort(1)
                .build();
    }
}
