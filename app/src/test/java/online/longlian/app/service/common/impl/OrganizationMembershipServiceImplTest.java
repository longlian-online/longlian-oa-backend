package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        service = new OrganizationMembershipServiceImpl(organizationMapper, organizationMemberMapper, userMapper);
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
    void shouldSuggestDefaultOrgWhenItIsEnabled() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).defaultOrgId(10L).build());
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        OrganizationMember member = member(100L, 10L, Status.ENABLED, "ORG_ADMIN");
        when(organizationMemberMapper.selectOne(any())).thenReturn(member);

        assertThat(service.suggestForLogin(1L)).isSameAs(member);
        verify(organizationMemberMapper, never()).selectList(any());
    }

    @Test
    void shouldSuggestEarliestEnabledOrgWhenDefaultIsUnavailable() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).defaultOrgId(10L).build());
        when(organizationMapper.selectById(10L)).thenReturn(Organization.builder().id(10L).status(Status.DISABLED).build());
        when(organizationMapper.selectById(20L)).thenReturn(enabledOrg(20L));
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(
                member(1L, 10L, Status.ENABLED, "ORG_USER"),
                member(2L, 20L, Status.ENABLED, "ORG_ADMIN")
        ));
        when(organizationMemberMapper.selectOne(any())).thenReturn(member(2L, 20L, Status.ENABLED, "ORG_ADMIN"));

        assertThat(service.suggestForLogin(1L).getOrgId()).isEqualTo(20L);

        InOrder order = inOrder(organizationMapper);
        order.verify(organizationMapper, org.mockito.Mockito.times(2)).selectById(10L);
        order.verify(organizationMapper).selectById(20L);
    }

    @Test
    void shouldSuggestEarliestMemberWhenDefaultOrgIsZero() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).defaultOrgId(0L).build());
        when(organizationMapper.selectById(20L)).thenReturn(enabledOrg(20L));
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(member(2L, 20L, Status.ENABLED, "ORG_USER")));
        when(organizationMemberMapper.selectOne(any())).thenReturn(member(2L, 20L, Status.ENABLED, "ORG_USER"));

        assertThat(service.suggestForLogin(1L).getOrgId()).isEqualTo(20L);
        verify(organizationMapper, never()).selectById(0L);
    }

    @Test
    void shouldSkipInvalidOrgIdsAndMissingOrganizationsWhileSuggesting() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).defaultOrgId(10L).build());
        when(organizationMapper.selectById(10L)).thenReturn(null);
        when(organizationMapper.selectById(20L)).thenReturn(enabledOrg(20L));
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(
                OrganizationMember.builder().id(1L).userId(1L).orgRole("ORG_USER").status(Status.ENABLED).build(),
                member(2L, 0L, Status.ENABLED, "ORG_USER"),
                member(3L, 20L, Status.ENABLED, "ORG_ADMIN")
        ));
        when(organizationMemberMapper.selectOne(any())).thenReturn(member(3L, 20L, Status.ENABLED, "ORG_ADMIN"));

        assertThat(service.suggestForLogin(1L).getOrgId()).isEqualTo(20L);
        verify(organizationMapper, never()).selectById(0L);
    }

    @Test
    void shouldRejectLoginSuggestionWhenNoEnabledOrganizationRemains() {
        when(userMapper.selectById(1L)).thenReturn(null);
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.suggestForLogin(1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("当前无可用组织");
    }

    @Test
    void shouldNotDependOnRedis() {
        assertThat(OrganizationMembershipServiceImpl.class.getDeclaredFields())
                .extracting(Field::getType)
                .extracting(Class::getName)
                .noneMatch(name -> name.contains("Redis"));
    }

    private Organization enabledOrg(long orgId) {
        return Organization.builder().id(orgId).status(Status.ENABLED).build();
    }

    private OrganizationMember member(long id, long orgId, Status status, String role) {
        return OrganizationMember.builder().id(id).orgId(orgId).userId(1L).orgRole(role).status(status).build();
    }
}
