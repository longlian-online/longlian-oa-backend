package online.longlian.app.service.orgadmin.impl.projecttype;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;
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
import java.util.Collections;
import java.util.List;

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
    void listProjectTypes_emptyPage_returnsEmptyResult() {
        ProjectTypeListParamsBO params = buildListParams();
        LambdaQueryWrapper<ProjectType> queryWrapper = new LambdaQueryWrapper<>();
        Page<ProjectType> projectTypePage = new Page<>(1, 10);
        projectTypePage.setRecords(Collections.emptyList());
        projectTypePage.setTotal(0L);
        when(queryBuilder.buildListQuery(params)).thenReturn(queryWrapper);
        when(projectTypeMapper.selectPage(any(Page.class), same(queryWrapper))).thenReturn(projectTypePage);

        PageResultBO<ProjectTypeListResultBO> result = service.listProjectTypes(params);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verifyNoInteractions(assembler);
    }

    @Test
    void listProjectTypes_nonEmptyPage_assemblesResult() {
        ProjectTypeListParamsBO params = buildListParams();
        LambdaQueryWrapper<ProjectType> queryWrapper = new LambdaQueryWrapper<>();
        ProjectType projectType = ProjectType.builder().id(10L).name("漫画").build();
        ProjectTypeListResultBO assembled = ProjectTypeListResultBO.builder().id(10L).name("漫画").build();
        Page<ProjectType> projectTypePage = new Page<>(1, 10);
        projectTypePage.setRecords(List.of(projectType));
        projectTypePage.setTotal(1L);
        when(queryBuilder.buildListQuery(params)).thenReturn(queryWrapper);
        when(projectTypeMapper.selectPage(any(Page.class), same(queryWrapper))).thenReturn(projectTypePage);
        when(assembler.assembleList(List.of(projectType))).thenReturn(List.of(assembled));

        PageResultBO<ProjectTypeListResultBO> result = service.listProjectTypes(params);

        assertThat(result.getList()).containsExactly(assembled);
        assertThat(result.getTotal()).isEqualTo(1L);
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
    void updateProjectType_nonExistentType_throwsDataNotExist() {
        when(projectTypeMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateProjectType(ProjectTypeUpdateParamsBO.builder()
                .typeId(10L).orgId(1L).name("新名称").build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));
        verify(projectTypeMapper, never()).update(any(), any());
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

    @Test
    void changeProjectTypeStatus_ownedType_updatesStatus() {
        when(projectTypeMapper.selectById(10L)).thenReturn(ProjectType.builder()
                .id(10L).orgId(1L).status(Status.DISABLED).build());

        service.changeProjectTypeStatus(ProjectTypeChangeStatusParamsBO.builder()
                .typeId(10L).orgId(1L).status(Status.ENABLED).build());

        verify(projectTypeMapper).update(isNull(), any());
    }

    @Test
    void changeProjectTypeStatus_nonExistentType_throwsDataNotExist() {
        when(projectTypeMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> service.changeProjectTypeStatus(ProjectTypeChangeStatusParamsBO.builder()
                .typeId(10L).orgId(1L).status(Status.ENABLED).build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));
        verify(projectTypeMapper, never()).update(any(), any());
    }

    @Test
    void changeProjectTypeStatus_otherOrganization_throwsUnauthorized() {
        when(projectTypeMapper.selectById(10L)).thenReturn(ProjectType.builder()
                .id(10L).orgId(2L).status(Status.DISABLED).build());

        assertThatThrownBy(() -> service.changeProjectTypeStatus(ProjectTypeChangeStatusParamsBO.builder()
                .typeId(10L).orgId(1L).status(Status.ENABLED).build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        verify(projectTypeMapper, never()).update(any(), any());
    }

    private ProjectTypeListParamsBO buildListParams() {
        return ProjectTypeListParamsBO.builder()
                .orgId(1L)
                .orderDir(SortDirection.DESC)
                .page(new PageParamsBO(1, 10))
                .build();
    }
}
