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
import online.longlian.app.service.common.DefaultOrganization;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private Organization enabledOrg(long orgId) {
        return Organization.builder().id(orgId).status(Status.ENABLED).build();
    }

    private OrganizationMember member(long id, long orgId, Status status, String role) {
        return OrganizationMember.builder().id(id).orgId(orgId).userId(1L).orgRole(role).status(status).build();
    }
}
