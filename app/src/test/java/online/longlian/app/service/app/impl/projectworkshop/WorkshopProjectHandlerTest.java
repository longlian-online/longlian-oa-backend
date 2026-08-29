package online.longlian.app.service.app.impl.projectworkshop;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.entity.ProjectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopProjectHandlerTest {

    @Mock
    private ProjectTypeMapper projectTypeMapper;
    @Mock
    private TaskTemplateMapper taskTemplateMapper;
    @Mock
    private TaskTemplateNodeMapper taskTemplateNodeMapper;

    private WorkshopProjectHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WorkshopProjectHandler(
                projectTypeMapper, taskTemplateMapper, taskTemplateNodeMapper, Clock.systemUTC());
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
}
