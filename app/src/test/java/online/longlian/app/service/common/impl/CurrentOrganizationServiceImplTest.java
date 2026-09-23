package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.CurrentOrganizationContextBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentOrganizationServiceImplTest {
    @Mock private UserMapper userMapper;
    @Mock private OrganizationMapper organizationMapper;
    @Mock private OrganizationMemberMapper organizationMemberMapper;

    private CurrentOrganizationServiceImpl service;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Organization.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OrganizationMember.class);
        service = new CurrentOrganizationServiceImpl(userMapper, organizationMapper, organizationMemberMapper);
    }

    @Test
    void shouldResolveDefaultOrganizationFromDatabase() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).defaultOrgId(10L).build());
        when(organizationMapper.selectById(10L)).thenReturn(Organization.builder().id(10L).status(Status.ENABLED).build());
        when(organizationMemberMapper.selectOne(any())).thenReturn(member(1L, 10L, "ORG_ADMIN"));

        CurrentOrganizationContextBO context = service.resolveCurrentOrgContext(1L, null);

        assertThat(context.getOrgId()).isEqualTo(10L);
        assertThat(context.getRoles()).containsExactly("ORG_ADMIN");
    }

    @Test
    void shouldResolveSwitchContextFromTargetMembership() {
        when(organizationMapper.selectById(20L)).thenReturn(Organization.builder().id(20L).status(Status.ENABLED).build());
        when(organizationMemberMapper.selectOne(any())).thenReturn(member(1L, 20L, "ORG_USER"));

        CurrentOrganizationContextBO context = service.switchCurrentOrg(1L, 20L);

        assertThat(context.getOrgId()).isEqualTo(20L);
        assertThat(context.getRoles()).containsExactly("ORG_USER");
    }

    @Test
    void shouldRejectUnavailableTargetOrganization() {
        when(organizationMapper.selectById(20L)).thenReturn(Organization.builder().id(20L).status(Status.DISABLED).build());

        assertThatThrownBy(() -> service.switchCurrentOrg(1L, 20L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织已被禁用");
    }

    private OrganizationMember member(Long userId, Long orgId, String role) {
        return OrganizationMember.builder().id(100L).userId(userId).orgId(orgId)
                .orgRole(role).status(Status.ENABLED).build();
    }
}
