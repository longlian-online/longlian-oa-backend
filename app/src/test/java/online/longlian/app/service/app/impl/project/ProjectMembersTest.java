package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.pojo.bo.app.ProjectMemberListParamsBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.service.common.OrganizationMemberDetails;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMembersTest {
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectWorkshopMapper projectWorkshopMapper;
    @Mock private OrganizationMembershipService organizationMembershipService;
    @Mock private ResourceService resourceService;
    @InjectMocks private ProjectServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProjectWorkshop.class);
    }

    /** 关联先去重，档案过滤后分页，资源只批量加载当前页。 */
    @Test
    void shouldDeduplicateMembersAndLoadOnlyPageAvatars() {
        visibleProject();
        when(projectWorkshopMapper.selectList(any())).thenReturn(List.of(
                association(1L), association(1L), association(2L), association(3L)));
        when(organizationMembershipService.listMemberProfiles(1L, List.of(1L, 2L, 3L))).thenReturn(List.of(
                new OrganizationMemberDetails(1L, "creator", 11L),
                new OrganizationMemberDetails(2L, "member", 12L)));
        when(resourceService.getResourceReadUrls(List.of(12L))).thenReturn(
                Map.of(12L, new ResourceReadUrlGetResultBO("avatar", 1L, "key")));

        var result = service.getProjectMembers(params(2, 1));
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getList()).singleElement().satisfies(member -> {
            assertThat(member.getUserId()).isEqualTo(2L);
            assertThat(member.getNickname()).isEqualTo("member");
            assertThat(member.getAvatarUrl()).isEqualTo("avatar");
            assertThat(member.getIsCreator()).isFalse();
        });
        verify(resourceService).getResourceReadUrls(List.of(12L));
        verify(resourceService, never()).getResourceReadUrl(any());
    }

    /** 负责人只按真实关联标记，未设置头像不应查询单资源。 */
    @Test
    void shouldMarkAssociatedCreatorWithoutAvatar() {
        visibleProject();
        when(projectWorkshopMapper.selectList(any())).thenReturn(List.of(association(1L)));
        when(organizationMembershipService.listMemberProfiles(1L, List.of(1L)))
                .thenReturn(List.of(new OrganizationMemberDetails(1L, "creator", null)));
        when(resourceService.getResourceReadUrls(List.of())).thenReturn(Map.of());
        var result = service.getProjectMembers(params(1, 12));
        assertThat(result.getList()).singleElement().satisfies(member -> {
            assertThat(member.getIsCreator()).isTrue();
            assertThat(member.getAvatarUrl()).isNull();
        });
    }

    /** 空关联不能凭负责人字段制造成员；超出最后一页保留总数。 */
    @Test
    void shouldReturnEmptyWithoutInventingCreator() {
        visibleProject();
        when(projectWorkshopMapper.selectList(any())).thenReturn(List.of());
        when(organizationMembershipService.listMemberProfiles(1L, List.of())).thenReturn(List.of());
        var result = service.getProjectMembers(params(Integer.MAX_VALUE, 100));
        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    /** 不存在、跨组织和禁用企划统一不可见，不能继续查询成员。 */
    @Test
    void shouldRejectInvisibleProjectsBeforeReadingMembers() {
        for (Project project : new Project[]{null,
                Project.builder().id(1L).orgId(2L).resourceStatus(Status.ENABLED).build(),
                Project.builder().id(1L).orgId(1L).resourceStatus(Status.DISABLED).build()}) {
            when(projectMapper.selectById(1L)).thenReturn(project);
            assertThatThrownBy(() -> service.getProjectMembers(params(1, 12))).isInstanceOf(AppException.class);
        }
        verifyNoInteractions(projectWorkshopMapper, organizationMembershipService, resourceService);
    }

    private void visibleProject() {
        when(projectMapper.selectById(1L)).thenReturn(Project.builder()
                .id(1L).orgId(1L).creatorId(1L).resourceStatus(Status.ENABLED).build());
    }

    private ProjectWorkshop association(long userId) {
        return ProjectWorkshop.builder().projectId(1L).userId(userId).build();
    }

    private ProjectMemberListParamsBO params(int pageNum, int pageSize) {
        return ProjectMemberListParamsBO.builder().projectId(1L).orgId(1L)
                .page(new PageParamsBO(pageNum, pageSize)).build();
    }
}
