package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.OrganizationMembershipParamsBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.DefaultOrganization;
import online.longlian.common.enumeration.Status;
import online.longlian.app.service.common.OrganizationMemberProfile;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationMembershipServiceImplTest {

    @Mock
    private OrganizationMapper organizationMapper;
    @Mock
    private OrganizationMemberMapper organizationMemberMapper;
    @Mock
    private UserMapper userMapper;

    private OrganizationMembershipServiceImpl service;
    private final OrganizationMembershipParamsBO membershipParams = new OrganizationMembershipParamsBO(7L, 12L);

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        service = new OrganizationMembershipServiceImpl(organizationMapper, organizationMemberMapper, userMapper);
    }

    /** 空 ID 列表不触发成员和用户查询。 */
    @Test
    void shouldSkipEmptyMemberProfileLookup() {
        assertThat(service.listMemberProfiles(1L, List.of())).isEmpty();
        verifyNoInteractions(organizationMemberMapper, userMapper);
    }

    /** 档案按组织和关联 ID 筛选，批量查用户，忽略已删除用户并回退空白昵称。 */
    @Test
    void shouldBatchLoadScopedMemberProfiles() {
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(
                OrganizationMember.builder().orgId(1L).userId(3L).build(),
                OrganizationMember.builder().orgId(1L).userId(2L).build(),
                OrganizationMember.builder().orgId(1L).userId(4L).build()));
        when(userMapper.selectBatchIds(List.of(3L, 2L, 4L))).thenReturn(List.of(
                User.builder().id(3L).nickname("  昵称  ").username("third").avatarFileId(30L).build(),
                User.builder().id(2L).nickname("  ").username("second").build()));

        var profiles = service.listMemberProfiles(1L, List.of(2L, 3L, 4L));
        assertThat(profiles).extracting(online.longlian.app.service.common.OrganizationMemberDetails::userId)
                .containsExactly(2L, 3L);
        assertThat(profiles).extracting(online.longlian.app.service.common.OrganizationMemberDetails::nickname)
                .containsExactly("second", "昵称");
        assertThat(profiles.get(1).avatarFileId()).isEqualTo(30L);
        ArgumentCaptor<LambdaQueryWrapper<OrganizationMember>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(organizationMemberMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("org_id", "user_id");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains(1L, 2L, 3L, 4L);
    }

    /** 没有匹配组织成员时不查询用户。 */
    @Test
    void shouldSkipUsersWhenNoOrganizationMembersMatch() {
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());
        assertThat(service.listMemberProfiles(1L, List.of(2L))).isEmpty();
        verifyNoInteractions(userMapper);
    }

    @Test
    void shouldRequireEnabledMember() {
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        OrganizationMember member = member(100L, 10L, Status.ENABLED, "ORG_USER");
        when(organizationMemberMapper.selectOne(any())).thenReturn(member);

        assertThat(service.requireEnabledMember(1L, 10L)).isSameAs(member);
    }

    @Test
    void shouldRejectRequireEnabledMemberForEachUnavailableCase() {
        assertThatThrownBy(() -> service.requireEnabledMember(1L, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织不能为空");
        assertThatThrownBy(() -> service.requireEnabledMember(1L, 0L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织ID不合法");
        assertThatThrownBy(() -> service.requireEnabledMember(1L, -1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织ID不合法");

        when(organizationMapper.selectById(10L)).thenReturn(null);
        assertThatThrownBy(() -> service.requireEnabledMember(1L, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织不存在");

        when(organizationMapper.selectById(10L)).thenReturn(Organization.builder().id(10L).status(Status.DISABLED).build());
        assertThatThrownBy(() -> service.requireEnabledMember(1L, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织已被禁用");

        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        when(organizationMemberMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.requireEnabledMember(1L, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("您不是该组织成员");

        when(organizationMemberMapper.selectOne(any())).thenReturn(member(100L, 10L, Status.DISABLED, "ORG_USER"));
        assertThatThrownBy(() -> service.requireEnabledMember(1L, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("您在该组织中的成员状态已被禁用");
    }

    @Test
    void shouldReturnEnabledDefaultOrganization() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).defaultOrgId(10L).build());
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        OrganizationMember member = member(100L, 10L, Status.ENABLED, "ORG_ADMIN");
        when(organizationMemberMapper.selectOne(any())).thenReturn(member);

        DefaultOrganization result = service.findDefault(1L);

        assertThat(result.orgId()).isEqualTo(10L);
        assertThat(result.orgRole()).isEqualTo("ORG_ADMIN");
        verify(organizationMemberMapper, never()).selectList(any());
    }

    @Test
    void shouldKeepStoredDefaultOrgWhenItIsDisabled() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).defaultOrgId(10L).build());
        when(organizationMapper.selectById(10L)).thenReturn(Organization.builder().id(10L).status(Status.DISABLED).build());

        DefaultOrganization result = service.findDefault(1L);

        assertThat(result.orgId()).isEqualTo(10L);
        assertThat(result.orgRole()).isNull();
        verify(organizationMemberMapper, never()).selectList(any());
        verify(organizationMemberMapper, never()).selectOne(any());
    }

    @Test
    void shouldReturnNoDefaultOrgWhenStoredIdIsZero() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).defaultOrgId(0L).build());

        DefaultOrganization result = service.findDefault(1L);

        assertThat(result.orgId()).isNull();
        assertThat(result.orgRole()).isNull();
        verifyNoInteractions(organizationMapper, organizationMemberMapper);
    }

    @Test
    void shouldReturnNoDefaultOrgWhenUserDoesNotExist() {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThat(service.findDefault(1L)).isEqualTo(new DefaultOrganization(null, null));
        verifyNoInteractions(organizationMapper, organizationMemberMapper);
    }


    @Test
    void shouldNotDependOnRedis() {
        assertThat(OrganizationMembershipServiceImpl.class.getDeclaredFields())
                .extracting(Field::getType)
                .extracting(Class::getName)
                .noneMatch(name -> name.contains("Redis"));
    }

    @Test
    void shouldRejectUnassignableMember() {
        assertThatThrownBy(() -> service.requireAssignableMember(null, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组员不能为空");
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        when(organizationMemberMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.requireAssignableMember(2L, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("所选组员不是当前组织成员");
        when(organizationMemberMapper.selectOne(any())).thenReturn(member(100L, 10L, Status.DISABLED, "ORG_USER"));
        assertThatThrownBy(() -> service.requireAssignableMember(2L, 10L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("所选组员已被禁用");
    }

    @Test
    void shouldRejectAssignableMemberWhenOrganizationIsMissingOrDisabled() {
        when(organizationMapper.selectById(10L)).thenReturn(null,
                Organization.builder().id(10L).status(Status.DISABLED).build());

        assertThatThrownBy(() -> service.requireAssignableMember(2L, 10L))
                .isInstanceOf(AppException.class).hasMessage("组织不可用");
        assertThatThrownBy(() -> service.requireAssignableMember(2L, 10L))
                .isInstanceOf(AppException.class).hasMessage("组织不可用");
        verifyNoInteractions(organizationMemberMapper, userMapper);
    }

    @Test
    void shouldRejectNonPositiveAssignableUserIdsWithoutQueryingOrganization() {
        assertThatThrownBy(() -> service.requireAssignableMember(0L, 10L))
                .isInstanceOf(AppException.class).hasMessage("组员不能为空");
        assertThatThrownBy(() -> service.requireAssignableMember(-1L, 10L))
                .isInstanceOf(AppException.class).hasMessage("组员不能为空");
        verifyNoInteractions(organizationMapper, organizationMemberMapper, userMapper);
    }

    @Test
    void shouldReturnEnabledAssignableMember() {
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        OrganizationMember member = member(100L, 10L, Status.ENABLED, "ORG_USER");
        when(organizationMemberMapper.selectOne(any())).thenReturn(member);

        assertThat(service.requireAssignableMember(1L, 10L)).isSameAs(member);
    }

    @Test
    void shouldReturnEmptyMembersWithoutLoadingUsers() {
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());

        assertThat(service.listEnabledMembers(10L)).isEmpty();
        verifyNoInteractions(userMapper, organizationMapper);
    }

    @Test
    void shouldListEnabledMembersByDisplayNameAndSkipMissingUsers() {
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(
                OrganizationMember.builder().id(1L).orgId(10L).userId(2L).status(Status.ENABLED).build(),
                OrganizationMember.builder().id(2L).orgId(10L).userId(9L).status(Status.ENABLED).build(),
                OrganizationMember.builder().id(3L).orgId(10L).userId(1L).status(Status.ENABLED).build()));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                User.builder().id(2L).username("ming").nickname(" 小明 ").build(),
                User.builder().id(1L).username("owner").nickname("").build()));

        assertThat(service.listEnabledMembers(10L)).containsExactly(
                new OrganizationMemberProfile(1L, "owner"),
                new OrganizationMemberProfile(2L, "小明"));
    }

    @Test
    void shouldListEnabledOwnerAndAdministratorsByUserId() {
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(
                OrganizationMember.builder().userId(8L).build(),
                OrganizationMember.builder().userId(2L).build(),
                OrganizationMember.builder().userId(8L).build(),
                OrganizationMember.builder().userId(null).build()));

        assertThat(service.listEnabledAdministratorIds(10L)).containsExactly(2L, 8L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<OrganizationMember>> query =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(organizationMemberMapper).selectList(query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("org_role");
        assertThat(query.getValue().getSqlSegment()).contains("status");
    }

    /** 缺失身份或非法组织不能触发管理员查询。 */
    @Test
    void shouldReturnFalseForAdministratorCheckWithoutValidIdentity() {
        assertThat(service.isEnabledAdministrator(null, 10L)).isFalse();
        assertThat(service.isEnabledAdministrator(1L, null)).isFalse();
        assertThat(service.isEnabledAdministrator(1L, 0L)).isFalse();
        assertThat(service.isEnabledAdministrator(1L, -1L)).isFalse();
        verifyNoInteractions(organizationMapper, organizationMemberMapper, userMapper);
    }

    /** 组织不存在或停用时，即使有成员记录也不能获得管理员权限。 */
    @Test
    void shouldReturnFalseForAdministratorOfUnavailableOrganization() {
        when(organizationMapper.selectById(10L)).thenReturn(null,
                Organization.builder().id(10L).status(Status.DISABLED).build());
        assertThat(service.isEnabledAdministrator(1L, 10L)).isFalse();
        assertThat(service.isEnabledAdministrator(1L, 10L)).isFalse();
        verifyNoInteractions(organizationMemberMapper, userMapper);
    }

    /** 管理员查询必须同时约束用户、当前组织、启用状态和 owner/admin 角色。 */
    @Test
    void shouldScopeEnabledAdministratorCheckToCurrentUserOrganizationStatusAndRoles() {
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        when(organizationMemberMapper.selectCount(any())).thenReturn(1L);
        assertThat(service.isEnabledAdministrator(7L, 10L)).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<OrganizationMember>> query =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(organizationMemberMapper).selectCount(query.capture());
        LambdaQueryWrapper<OrganizationMember> wrapper = query.getValue();
        assertThat(wrapper.getSqlSegment()).contains("user_id", "org_id", "status", "org_role");
        assertThat(wrapper.getParamNameValuePairs().values()).containsExactlyInAnyOrder(
                7L, 10L, Status.ENABLED, "ORG_OWNER", "ORG_ADMIN");
        verifyNoInteractions(userMapper);
    }

    /** 普通成员、停用成员或跨组织管理员均不能匹配当前组织的管理员查询。 */
    @Test
    void shouldReturnFalseWhenNoEnabledAdministratorMatches() {
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        when(organizationMemberMapper.selectCount(any())).thenReturn(0L);
        assertThat(service.isEnabledAdministrator(7L, 10L)).isFalse();
    }

    private Organization enabledOrg(long orgId) {
        return Organization.builder().id(orgId).status(Status.ENABLED).build();
    }

    private OrganizationMember member(long id, long orgId, Status status, String role) {
        return OrganizationMember.builder().id(id).orgId(orgId).userId(1L).orgRole(role).status(status).build();
    }

    @Test
    void shouldReadDisabledMembershipWithoutRejectingIt() {
        stubMembershipStatus();
        when(organizationMemberMapper.selectOne(any())).thenReturn(OrganizationMember.builder().status(Status.DISABLED).build());
        var result = service.getOwnMembershipStatus(membershipParams);
        assertThat(result.userEnabled()).isTrue();
        assertThat(result.organizationEnabled()).isTrue();
        assertThat(result.memberEnabled()).isFalse();
        assertThat(result.enabled()).isFalse();
    }

    @Test
    void shouldReadDisabledUserAndOrganizationStates() {
        stubMembershipStatus();
        when(userMapper.selectById(7L)).thenReturn(User.builder().status(Status.DISABLED).build());
        when(organizationMapper.selectById(12L)).thenReturn(Organization.builder().status(Status.DISABLED).build());
        var result = service.getOwnMembershipStatus(membershipParams);
        assertThat(result.userEnabled()).isFalse();
        assertThat(result.organizationEnabled()).isFalse();
        assertThat(result.memberEnabled()).isTrue();
    }

    @Test
    void shouldRejectMissingMembership() {
        stubMembershipStatus();
        when(organizationMemberMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.getOwnMembershipStatus(membershipParams)).isInstanceOf(AppException.class);
    }

    /** 本人设置页仍能展示停用组织，按组织 ID 稳定排列。 */
    @Test
    void shouldListDisabledOrganizationsInStableOrder() {
        when(userMapper.selectById(7L)).thenReturn(User.builder().status(Status.ENABLED).build());
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(
                OrganizationMember.builder().orgId(12L).status(Status.DISABLED).build(),
                OrganizationMember.builder().orgId(13L).status(Status.ENABLED).build()));
        when(organizationMapper.selectBatchIds(List.of(12L, 13L))).thenReturn(List.of(
                Organization.builder().id(13L).name("乙").status(Status.ENABLED).build(),
                Organization.builder().id(12L).name("甲").status(Status.DISABLED).build()));
        assertThat(service.listOwnOrganizations(7L)).extracting(result -> result.id()).containsExactly(12L, 13L);
    }

    /** 没有成员关系时不再查组织表。 */
    @Test
    void shouldAvoidOrganizationQueryWhenUserHasNoMemberships() {
        when(userMapper.selectById(7L)).thenReturn(User.builder().status(Status.ENABLED).build());
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());
        assertThat(service.listOwnOrganizations(7L)).isEmpty();
        verify(organizationMapper, never()).selectBatchIds(any());
    }

    /** 停用账号不能通过本人设置页读取组织。 */
    @Test
    void shouldRejectDisabledUserBeforeListingOrganizations() {
        when(userMapper.selectById(7L)).thenReturn(User.builder().status(Status.DISABLED).build());
        assertThatThrownBy(() -> service.listOwnOrganizations(7L)).isInstanceOf(AppException.class);
        verify(organizationMemberMapper, never()).selectList(any());
    }

    private void stubMembershipStatus() {
        when(userMapper.selectById(7L)).thenReturn(User.builder().id(7L).status(Status.ENABLED).build());
        when(organizationMapper.selectById(12L)).thenReturn(Organization.builder().id(12L).status(Status.ENABLED).build());
        when(organizationMemberMapper.selectOne(any())).thenReturn(OrganizationMember.builder()
                .userId(7L).orgId(12L).status(Status.ENABLED).build());
    }
}
