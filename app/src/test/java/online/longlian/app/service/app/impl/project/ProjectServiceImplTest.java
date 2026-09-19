package online.longlian.app.service.app.impl.project;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.pojo.bo.app.ProjectCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectUpdateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopRemoveParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.resource.ResourceService;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectTypeMapper projectTypeMapper;
    @Mock
    private ProjectWorkshopMapper projectWorkshopMapper;
    @Mock
    private ResourceService resourceService;
    @Mock
    private ProjectQueryBuilder projectQueryBuilder;
    @Mock
    private ProjectAssembler projectAssembler;
    @Mock
    private ProjectProgressHandler projectProgressHandler;
    @Mock
    private LockService lockService;

    private ProjectServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProjectServiceImpl(
                projectMapper,
                projectTypeMapper,
                projectWorkshopMapper,
                resourceService,
                projectQueryBuilder,
                projectAssembler,
                projectProgressHandler,
                lockService,
                Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void createProject_addsCreatorToWorkshop() {
        when(projectTypeMapper.selectById(10L)).thenReturn(
                ProjectType.builder().id(10L).orgId(1L).status(Status.ENABLED).build());
        doAnswer(invocation -> {
            invocation.getArgument(0, Project.class).setId(100L);
            return 1;
        }).when(projectMapper).insert(any(Project.class));

        service.createProject(ProjectCreateParamsBO.builder()
                .orgId(1L)
                .creatorId(2L)
                .title("测试企划")
                .alias("alias")
                .typeId(10L)
                .metadata("{}")
                .description("描述")
                .coverFileId(20L)
                .build());

        ArgumentCaptor<ProjectWorkshopAddParamsBO> workshopCaptor =
                ArgumentCaptor.forClass(ProjectWorkshopAddParamsBO.class);
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getResourceStatus()).isEqualTo(Status.ENABLED);
        verify(projectProgressHandler).addToWorkshop(workshopCaptor.capture());
        assertThat(workshopCaptor.getValue().getProjectId()).isEqualTo(100L);
        assertThat(workshopCaptor.getValue().getUserId()).isEqualTo(2L);
        assertThat(workshopCaptor.getValue().getOrgId()).isEqualTo(1L);
    }

    @Test
    void createProject_invalidType_doesNotAddToWorkshop() {
        when(projectTypeMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> service.createProject(ProjectCreateParamsBO.builder()
                .orgId(1L)
                .creatorId(2L)
                .title("测试企划")
                .alias("alias")
                .typeId(10L)
                .metadata("{}")
                .description("描述")
                .coverFileId(20L)
                .build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));

        verify(projectMapper, never()).insert(any(Project.class));
        verify(projectProgressHandler, never()).addToWorkshop(any());
    }

    @Test
    void updateProject_nonCreator_throwsUnauthorized() {
        when(projectMapper.selectById(100L)).thenReturn(Project.builder()
                .id(100L)
                .orgId(1L)
                .creatorId(3L)
                .build());

        assertThatThrownBy(() -> service.updateProject(ProjectUpdateParamsBO.builder()
                .projectId(100L)
                .orgId(1L)
                .userId(2L)
                .title("新标题")
                .build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    @Test
    void addToWorkshop_projectFromAnotherOrganization_throwsNotFound() {
        when(projectMapper.selectById(100L)).thenReturn(Project.builder()
                .id(100L)
                .orgId(2L)
                .build());

        assertThatThrownBy(() -> service.addToWorkshop(ProjectWorkshopAddParamsBO.builder()
                .projectId(100L)
                .userId(2L)
                .orgId(1L)
                .build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));

        verify(projectProgressHandler, never()).addToWorkshop(any());
    }

    @Test
    void removeFromWorkshop_missingProject_throwsNotFound() {
        when(projectMapper.selectById(100L)).thenReturn(null);

        assertThatThrownBy(() -> service.removeFromWorkshop(ProjectWorkshopRemoveParamsBO.builder()
                .projectId(100L)
                .userId(2L)
                .orgId(1L)
                .build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    @Test
    void getProjectDetail_disabledProject_throwsNotFound() {
        when(projectMapper.selectById(100L)).thenReturn(Project.builder()
                .id(100L)
                .orgId(1L)
                .resourceStatus(Status.DISABLED)
                .build());

        assertThatThrownBy(() -> service.getProjectDetail(100L, 2L, 1L))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }
}
