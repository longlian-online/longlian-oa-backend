package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.pojo.bo.app.ProjectCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectDetailResultBO;
import online.longlian.app.pojo.bo.app.ProjectProgressBO;
import online.longlian.app.pojo.bo.app.ProjectUpdateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectListResultBO;
import online.longlian.app.pojo.bo.app.ProjectSearchParamsBO;
import online.longlian.app.pojo.bo.app.ProjectSearchResultBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.common.ResourceBindParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopRemoveParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.resource.ResourceService;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), Project.class);
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

    /** 非法 metadata 不应创建项目或绑定资源。 */
    @Test
    void shouldRejectInvalidMetadataOnCreate() {
        when(projectTypeMapper.selectById(10L)).thenReturn(
                ProjectType.builder().id(10L).orgId(1L).status(Status.ENABLED).build());
        assertThatThrownBy(() -> service.createProject(ProjectCreateParamsBO.builder()
                .orgId(1L).typeId(10L).metadata("{\"tags\":[{\"key\":\"作者\",\"value\":1}]}").build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
        verify(projectMapper, never()).insert(any(Project.class));
        verify(resourceService, never()).bindBizResource(any());
    }

    /** 非法 metadata 不应修改现有项目或封面绑定。 */
    @Test
    void shouldRejectInvalidMetadataOnUpdate() {
        when(projectMapper.selectById(100L)).thenReturn(Project.builder()
                .id(100L).orgId(1L).creatorId(2L).resourceStatus(Status.ENABLED).build());
        assertThatThrownBy(() -> service.updateProject(ProjectUpdateParamsBO.builder()
                .projectId(100L).orgId(1L).userId(2L)
                .metadata("{\"tags\":[{\"key\":\"作者\",\"value\":\"山田\",\"extra\":true}]}").build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
        verify(projectMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(resourceService, never()).bindBizResource(any());
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
                .metadata("{\"tags\":[]}")
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
                .metadata("{\"tags\":[]}")
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
                .resourceStatus(Status.ENABLED)
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
    void updateProject_withoutCoverFileId_preservesCoverAndBinding() {
        when(projectMapper.selectById(100L)).thenReturn(Project.builder()
                .id(100L).orgId(1L).creatorId(2L)
                .coverFileId(20L).resourceStatus(Status.ENABLED).build());

        service.updateProject(ProjectUpdateParamsBO.builder()
                .projectId(100L).orgId(1L).userId(2L)
                .title("新标题").alias("new_alias")
                .metadata("{\"tags\":[]}").description("新描述").build());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> updateCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(projectMapper).update(any(), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getSqlSet())
                .contains("title", "alias", "metadata", "description", "updated_at")
                .doesNotContain("cover_file_id");
        verify(resourceService, never()).bindBizResource(any());
    }

    @Test
    void updateProject_withCoverFileId_replacesCoverAndBinding() {
        when(projectMapper.selectById(100L)).thenReturn(Project.builder()
                .id(100L).orgId(1L).creatorId(2L)
                .coverFileId(20L).resourceStatus(Status.ENABLED).build());

        service.updateProject(ProjectUpdateParamsBO.builder()
                .projectId(100L).orgId(1L).userId(2L)
                .title("新标题").alias("new_alias")
                .metadata("{\"tags\":[]}").description("新描述")
                .coverFileId(21L).build());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> updateCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(projectMapper).update(any(), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getSqlSet()).contains("cover_file_id");
        assertThat(updateCaptor.getValue().getParamNameValuePairs().values()).contains(21L);
        ArgumentCaptor<ResourceBindParamsBO> bindCaptor = ArgumentCaptor.forClass(ResourceBindParamsBO.class);
        verify(resourceService).bindBizResource(bindCaptor.capture());
        assertThat(bindCaptor.getValue().getResourceId()).isEqualTo(21L);
        assertThat(bindCaptor.getValue().getReplacedResourceId()).isEqualTo(20L);
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
    void removeFromWorkshop_creator_throwsUnauthorized() {
        when(projectMapper.selectById(100L)).thenReturn(Project.builder()
                .id(100L)
                .orgId(1L)
                .creatorId(2L)
                .resourceStatus(Status.ENABLED)
                .build());

        assertThatThrownBy(() -> service.removeFromWorkshop(ProjectWorkshopRemoveParamsBO.builder()
                .projectId(100L)
                .userId(2L)
                .orgId(1L)
                .build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));

        verify(projectWorkshopMapper, never()).update(any(), any());
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

    @Test
    void getProjectDetail_returnsCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 1, 8, 30);
        when(projectMapper.selectById(100L)).thenReturn(Project.builder()
                .id(100L)
                .orgId(1L)
                .creatorId(2L)
                .title("测试企划")
                .resourceStatus(Status.ENABLED)
                .createdAt(createdAt)
                .build());
        when(projectWorkshopMapper.selectCount(any())).thenReturn(0L);
        when(projectProgressHandler.computeProgress(100L)).thenReturn(ProjectProgressBO.builder()
                .progressPercent(0)
                .claimedTaskCount(0)
                .pendingTaskCount(0)
                .build());

        ProjectDetailResultBO result = service.getProjectDetail(100L, 2L, 1L);

        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void updateProject_disabledProject_throwsNotFound() {
        when(projectMapper.selectById(100L)).thenReturn(disabledProject());

        assertThatThrownBy(() -> service.updateProject(ProjectUpdateParamsBO.builder()
                .projectId(100L)
                .orgId(1L)
                .userId(2L)
                .title("新标题")
                .build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));

        verify(projectMapper, never()).update(any(), any());
    }

    @Test
    void addToWorkshop_disabledProject_throwsNotFound() {
        when(projectMapper.selectById(100L)).thenReturn(disabledProject());

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
    void removeFromWorkshop_disabledProject_throwsNotFound() {
        when(projectMapper.selectById(100L)).thenReturn(disabledProject());

        assertThatThrownBy(() -> service.removeFromWorkshop(ProjectWorkshopRemoveParamsBO.builder()
                .projectId(100L)
                .userId(2L)
                .orgId(1L)
                .build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));

        verify(projectWorkshopMapper, never()).update(any(), any());
    }

    private Project disabledProject() {
        return Project.builder()
                .id(100L)
                .orgId(1L)
                .creatorId(2L)
                .resourceStatus(Status.DISABLED)
                .build();
    }

    @Test
    void getProjectList_searchUsesDatabasePaginationAndOnlyReordersReturnedMetadata() {
        String original = "{\"tags\":[{\"key\":\"author\",\"value\":\"山田\"},{\"key\":\"series\",\"value\":\"星海\"}]}";
        Project project = Project.builder().id(1L).metadata(original).build();
        ProjectSearchResultBO row = new ProjectSearchResultBO();
        row.setProject(project);
        row.setMatchedMetadataPaths("[\"$.tags[1].value\"]");
        Page<ProjectSearchResultBO> page = new Page<>(2, 10, 12);
        page.setRecords(List.of(row));
        when(projectMapper.selectSearchPage(any(), any())).thenReturn(page);
        when(projectAssembler.assembleList(List.of(project))).thenReturn(List.of(
                ProjectListResultBO.builder().id(1L).metadata(original).build()));

        PageResultBO<ProjectListResultBO> result = service.getProjectList(ProjectListParamsBO.builder()
                .orgId(7L).keyword(" 星海 ").page(new PageParamsBO(2, 10)).build());

        ArgumentCaptor<ProjectSearchParamsBO> searchCaptor = ArgumentCaptor.forClass(ProjectSearchParamsBO.class);
        verify(projectMapper).selectSearchPage(any(), searchCaptor.capture());
        assertThat(searchCaptor.getValue().getOrgId()).isEqualTo(7L);
        assertThat(searchCaptor.getValue().getKeyword()).isEqualTo("星海");
        assertThat(result.getTotal()).isEqualTo(12);
        assertThat(com.alibaba.fastjson2.JSON.parseObject(result.getList().getFirst().getMetadata())
                .getJSONArray("tags").getJSONObject(0).getString("key")).isEqualTo("series");
        assertThat(project.getMetadata()).isEqualTo(original);
        verify(projectMapper, never()).selectPage(any(), any());
    }

    @Test
    void getProjectList_emptySearchPagePreservesTotalWithoutAssembly() {
        Page<ProjectSearchResultBO> page = new Page<>(3, 10, 12);
        when(projectMapper.selectSearchPage(any(), any())).thenReturn(page);

        PageResultBO<ProjectListResultBO> result = service.getProjectList(ProjectListParamsBO.builder()
                .orgId(1L).keyword("database").page(new PageParamsBO(3, 10)).build());

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(12);
        verify(projectAssembler, never()).assembleList(any());
    }

    @Test
    void getProjectList_blankKeywordKeepsOriginalListQueryAndMetadata() {
        ProjectListParamsBO params = ProjectListParamsBO.builder()
                .orgId(1L).keyword("  ").orderDir(SortDirection.DESC).page(new PageParamsBO(1, 10)).build();
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        Page<Project> page = new Page<>(1, 10, 1);
        Project project = Project.builder().id(1L).metadata("{ \"tags\": [] }").build();
        page.setRecords(List.of(project));
        when(projectQueryBuilder.buildListQuery(params, null)).thenReturn(wrapper);
        when(projectMapper.selectPage(any(), any())).thenReturn(page);
        when(projectAssembler.assembleList(List.of(project))).thenReturn(List.of(
                ProjectListResultBO.builder().metadata(project.getMetadata()).build()));

        PageResultBO<ProjectListResultBO> result = service.getProjectList(params);

        assertThat(result.getList().getFirst().getMetadata()).isEqualTo("{ \"tags\": [] }");
        verify(projectMapper, never()).selectSearchPage(any(), any());
    }

    @Test
    void getProjectList_invalidTypeFailsBeforeSearching() {
        when(projectTypeMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.getProjectList(ProjectListParamsBO.builder()
                .orgId(1L).projectType("不存在").keyword("星海").page(new PageParamsBO(1, 10)).build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
        verify(projectMapper, never()).selectSearchPage(any(), any());
    }
}
