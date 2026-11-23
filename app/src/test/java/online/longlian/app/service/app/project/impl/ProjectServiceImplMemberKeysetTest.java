package online.longlian.app.service.app.project.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.pojo.bo.app.project.ProjectMemberKeysetItemBO;
import online.longlian.app.pojo.bo.app.project.ProjectMemberKeysetParamsBO;
import online.longlian.app.pojo.bo.app.project.ProjectMemberKeysetResultBO;
import online.longlian.app.pojo.bo.app.project.ProjectMemberSubmissionStatBO;
import online.longlian.app.pojo.bo.common.OrganizationMemberDetailsResultBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.service.app.mytask.ClaimedTaskLoadService;
import online.longlian.app.service.app.taskinstance.impl.ProjectMemberSubmissionStats;
import online.longlian.app.service.organization.OrganizationMembershipService;
import online.longlian.app.service.orgadmin.projecttype.ProjectTypeCustomFormService;
import online.longlian.app.service.resource.ResourceService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 企划成员键集分页单元测试：userId 键集、org 可见性校验、hasMore 与统计口径。
 */
class ProjectServiceImplMemberKeysetTest {

    private static final Long PROJECT_ID = 41000L;
    private static final Long USER_ID = 9L;
    private static final Long ORG_ID = 8L;
    private static final LocalDateTime JOINED = LocalDateTime.of(2026, 1, 2, 9, 0, 0);

    private ProjectMapper projectMapper;
    private ProjectWorkshopMapper projectWorkshopMapper;
    private OrganizationMembershipService organizationMembershipService;
    private ProjectMemberSubmissionStats projectMemberSubmissionStats;
    private ProjectServiceImpl service;

    @BeforeEach
    void setUp() {
        // LambdaQueryWrapper 按列名取元数据，单测环境需先注册表信息
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ProjectWorkshop.class);
        projectMapper = mock(ProjectMapper.class);
        ProjectTypeMapper projectTypeMapper = mock(ProjectTypeMapper.class);
        projectWorkshopMapper = mock(ProjectWorkshopMapper.class);
        ItemMapper itemMapper = mock(ItemMapper.class);
        ResourceService resourceService = mock(ResourceService.class);
        ProjectQueryBuilder projectQueryBuilder = mock(ProjectQueryBuilder.class);
        ProjectAssembler projectAssembler = mock(ProjectAssembler.class);
        ProjectProgressHandler projectProgressHandler = mock(ProjectProgressHandler.class);
        ProjectAttachmentHandler projectAttachmentHandler = mock(ProjectAttachmentHandler.class);
        ProjectTypeCustomFormService projectTypeCustomFormService = mock(ProjectTypeCustomFormService.class);
        organizationMembershipService = mock(OrganizationMembershipService.class);
        projectMemberSubmissionStats = mock(ProjectMemberSubmissionStats.class);
        when(projectMemberSubmissionStats.forMembers(any(), anyList())).thenReturn(Map.of());
        service = new ProjectServiceImpl(projectMapper, projectTypeMapper, projectWorkshopMapper,
                itemMapper, resourceService, projectQueryBuilder, projectAssembler,
                projectProgressHandler, projectTypeCustomFormService, projectAttachmentHandler,
                organizationMembershipService, mock(ClaimedTaskLoadService.class),
                projectMemberSubmissionStats, Clock.systemUTC());
    }

    private ProjectWorkshop workshop(long userId) {
        ProjectWorkshop row = new ProjectWorkshop();
        row.setProjectId(PROJECT_ID);
        row.setUserId(userId);
        row.setCreatedAt(JOINED);
        return row;
    }

    private void members(List<Long> userIds) {
        when(organizationMembershipService.listMemberProfiles(eq(ORG_ID), anyList()))
                .thenReturn(userIds.stream()
                        .map(id -> new OrganizationMemberDetailsResultBO(id, "成员" + id, null))
                        .toList());
    }

    private void visibleProject(long orgId, long creatorId) {
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setOrgId(orgId);
        project.setCreatorId(creatorId);
        project.setResourceStatus(online.longlian.common.enumeration.Status.ENABLED);
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
    }

    @Test
    void shouldPageMembersByUserIdWithStats() {
        visibleProject(ORG_ID, USER_ID);
        when(projectWorkshopMapper.selectList(any())).thenReturn(List.of(
                workshop(44002L), workshop(44003L), workshop(44004L)));
        members(List.of(44002L, 44003L, 44004L));
        when(projectMemberSubmissionStats.forMembers(eq(PROJECT_ID), anyList())).thenReturn(Map.of(
                44002L, new ProjectMemberSubmissionStatBO(JOINED.plusHours(1), 3)));

        ProjectMemberKeysetResultBO page1 = service.getProjectMembersKeyset(
                new ProjectMemberKeysetParamsBO(PROJECT_ID, ORG_ID, null, 2));
        assertThat(page1.list()).extracting(ProjectMemberKeysetItemBO::userId).containsExactly(44002L, 44003L);
        assertThat(page1.hasMore()).isTrue();
        assertThat(page1.lastUserId()).isEqualTo(44003L);

        ProjectMemberKeysetItemBO first = page1.list().get(0);
        assertThat(first.nickname()).isEqualTo("成员44002");
        assertThat(first.joinedAt()).isEqualTo(JOINED);
        assertThat(first.creator()).isFalse();
        assertThat(first.completedCount()).isEqualTo(3);
        assertThat(first.lastSubmittedAt()).isEqualTo(JOINED.plusHours(1));

        ProjectMemberKeysetItemBO second = page1.list().get(1);
        assertThat(second.completedCount()).isZero();
        assertThat(second.lastSubmittedAt()).isNull();

        // 第二页取到游标之后
        ProjectMemberKeysetResultBO page2 = service.getProjectMembersKeyset(
                new ProjectMemberKeysetParamsBO(PROJECT_ID, ORG_ID, page1.lastUserId(), 2));
        assertThat(page2.list()).extracting(ProjectMemberKeysetItemBO::userId).containsExactly(44004L);
        assertThat(page2.hasMore()).isFalse();
        assertThat(page2.lastUserId()).isEqualTo(44004L);
    }

    @Test
    void shouldDropProfilesMissingFromOrgMembers() {
        visibleProject(ORG_ID, USER_ID);
        // 工坊里有 44002、44003，但 44003 已不在组织成员档案中
        when(projectWorkshopMapper.selectList(any())).thenReturn(List.of(workshop(44002L), workshop(44003L)));
        members(List.of(44002L));

        ProjectMemberKeysetResultBO page = service.getProjectMembersKeyset(
                new ProjectMemberKeysetParamsBO(PROJECT_ID, ORG_ID, null, 50));
        assertThat(page.list()).extracting(ProjectMemberKeysetItemBO::userId).containsExactly(44002L);
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void shouldMarkCreatorAndReturnEmptyForNoWorkshopRows() {
        visibleProject(ORG_ID, 44005L);
        when(projectWorkshopMapper.selectList(any())).thenReturn(List.of(workshop(44005L)));
        members(List.of(44005L));

        ProjectMemberKeysetResultBO page = service.getProjectMembersKeyset(
                new ProjectMemberKeysetParamsBO(PROJECT_ID, ORG_ID, null, 50));
        assertThat(page.list()).hasSize(1);
        assertThat(page.list().get(0).creator()).isTrue();

        when(projectWorkshopMapper.selectList(any())).thenReturn(List.of());
        ProjectMemberKeysetResultBO empty = service.getProjectMembersKeyset(
                new ProjectMemberKeysetParamsBO(PROJECT_ID, ORG_ID, null, 50));
        assertThat(empty.list()).isEmpty();
        assertThat(empty.hasMore()).isFalse();
        assertThat(empty.lastUserId()).isNull();
    }

    @Test
    void shouldFailCrossOrgProjectWithNotFoundSemantics() {
        visibleProject(ORG_ID + 1, USER_ID);

        // 跨组织企划按站内数据不存在拒绝，开放面映射为 404
        assertThatThrownBy(() -> service.getProjectMembersKeyset(
                new ProjectMemberKeysetParamsBO(PROJECT_ID, ORG_ID, null, 50)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("企划不存在");
    }
}
