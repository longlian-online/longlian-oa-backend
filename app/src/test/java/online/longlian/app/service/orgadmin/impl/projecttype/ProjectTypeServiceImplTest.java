package online.longlian.app.service.orgadmin.impl.projecttype;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeUpdateParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.common.enumeration.Status;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectTypeServiceImplTest {

    @Mock
    private ProjectTypeMapper projectTypeMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectTypeQueryBuilder queryBuilder;
    @Mock
    private ProjectTypeAssembler assembler;

    private ProjectTypeServiceImpl service;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ProjectType.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Project.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new ProjectTypeServiceImpl(projectTypeMapper, projectMapper, clock, queryBuilder, assembler);
    }

    @Test
    void createProjectType_trimsNameBeforeInsert() {
        when(projectTypeMapper.selectCount(any())).thenReturn(0L);

        service.createProjectType(ProjectTypeCreateParamsBO.builder()
                .orgId(1L).creatorId(2L).name("  漫画  ").build());

        ArgumentCaptor<ProjectType> captor = ArgumentCaptor.forClass(ProjectType.class);
        verify(projectTypeMapper).insert(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("漫画");
        assertThat(captor.getValue().getStatus()).isEqualTo(Status.ENABLED);
    }

    @Test
    void createProjectType_duplicateName_throwsParameterError() {
        when(projectTypeMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createProjectType(ProjectTypeCreateParamsBO.builder()
                .orgId(1L).creatorId(2L).name("漫画").build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
        verify(projectTypeMapper, never()).insert(any(ProjectType.class));
    }

    @Test
    void updateProjectType_ownedType_updatesName() {
        when(projectTypeMapper.selectById(10L)).thenReturn(ProjectType.builder()
                .id(10L).orgId(1L).name("旧名称").status(Status.ENABLED).build());
        when(projectTypeMapper.selectCount(any())).thenReturn(0L);

        service.updateProjectType(ProjectTypeUpdateParamsBO.builder()
                .typeId(10L).orgId(1L).name("  新名称  ").build());

        verify(projectTypeMapper).update(isNull(), any());
    }

    @Test
    void updateProjectType_duplicateName_throwsParameterError() {
        when(projectTypeMapper.selectById(10L)).thenReturn(ProjectType.builder()
                .id(10L).orgId(1L).name("旧名称").status(Status.ENABLED).build());
        when(projectTypeMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.updateProjectType(ProjectTypeUpdateParamsBO.builder()
                .typeId(10L).orgId(1L).name("新名称").build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
        verify(projectTypeMapper, never()).update(any(), any());
    }

    @Test
    void updateProjectType_otherOrganization_throwsUnauthorized() {
        when(projectTypeMapper.selectById(10L)).thenReturn(ProjectType.builder()
                .id(10L).orgId(2L).name("名称").status(Status.ENABLED).build());

        assertThatThrownBy(() -> service.updateProjectType(ProjectTypeUpdateParamsBO.builder()
                .typeId(10L).orgId(1L).name("新名称").build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    @Test
    void deleteProjectType_withoutProjects_softDeletesType() {
        when(projectTypeMapper.selectById(10L)).thenReturn(ProjectType.builder()
                .id(10L).orgId(1L).name("名称").status(Status.ENABLED).build());
        when(projectMapper.selectCount(any())).thenReturn(0L);

        service.deleteProjectType(ProjectTypeDeleteParamsBO.builder().typeId(10L).orgId(1L).build());

        verify(projectTypeMapper).deleteById(10L);
    }

    @Test
    void deleteProjectType_withProjects_throwsParameterError() {
        when(projectTypeMapper.selectById(10L)).thenReturn(ProjectType.builder()
                .id(10L).orgId(1L).name("名称").status(Status.ENABLED).build());
        when(projectMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteProjectType(ProjectTypeDeleteParamsBO.builder()
                .typeId(10L).orgId(1L).build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
        verify(projectTypeMapper, never()).deleteById(any());
    }
}
