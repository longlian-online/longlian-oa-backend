package online.longlian.app.service.orgadmin.impl.project;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.pojo.bo.orgadmin.ProjectChangeStatusParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.common.enumeration.ProjectStatus;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectQueryBuilder projectQueryBuilder;
    @Mock
    private ProjectAssembler projectAssembler;

    private ProjectServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), Project.class);
        service = new ProjectServiceImpl(
                projectMapper,
                Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneOffset.UTC),
                projectQueryBuilder,
                projectAssembler
        );
    }

    @Test
    void changeProjectStatus_ownedProject_updatesOnlyResourceStatus() {
        when(projectMapper.selectById(10L)).thenReturn(Project.builder()
                .id(10L)
                .orgId(1L)
                .status(ProjectStatus.COMPLETED)
                .resourceStatus(Status.ENABLED)
                .build());

        service.changeProjectStatus(ProjectChangeStatusParamsBO.builder()
                .projectId(10L)
                .orgId(1L)
                .status(Status.DISABLED)
                .build());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> wrapperCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(projectMapper).update(isNull(), wrapperCaptor.capture());
        LambdaUpdateWrapper<?> wrapper = wrapperCaptor.getValue();
        assertThat(wrapper.getSqlSet()).contains("resource_status");
        assertThat(wrapper.getSqlSet().replace("resource_status", "")).doesNotContain("status");
        assertThat(wrapper.getParamNameValuePairs()).containsValue(Status.DISABLED);
    }

    @Test
    void changeProjectStatus_missingProject_throwsDataNotExist() {
        when(projectMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> service.changeProjectStatus(ProjectChangeStatusParamsBO.builder()
                .projectId(10L).orgId(1L).status(Status.DISABLED).build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));

        verify(projectMapper, never()).update(any(), any());
    }

    @Test
    void changeProjectStatus_otherOrganization_throwsUnauthorized() {
        when(projectMapper.selectById(10L)).thenReturn(Project.builder().id(10L).orgId(2L).build());

        assertThatThrownBy(() -> service.changeProjectStatus(ProjectChangeStatusParamsBO.builder()
                .projectId(10L).orgId(1L).status(Status.DISABLED).build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));

        verify(projectMapper, never()).update(any(), any());
    }
}
