package online.longlian.app.service.common;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.*;
import online.longlian.app.pojo.entity.*;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrganizationAuthorizationServiceTest {
    private final OrganizationMapper organizations = mock(OrganizationMapper.class);
    private final OrganizationMemberMapper members = mock(OrganizationMemberMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final OrganizationAuthorizationService service = new OrganizationAuthorizationService(organizations, members, users);

    @BeforeEach
    void setUp() {
        when(members.selectList(any())).thenReturn(List.of(
                OrganizationMember.builder().orgRole("ORG_OWNER").status(Status.ENABLED).build()));
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Organization.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
    }

    @Test
    void shouldRejectDisabledOrganizationForGovernance() {
        when(organizations.selectOne(any())).thenReturn(Organization.builder().id(1L).status(Status.DISABLED).build());
        assertThatThrownBy(() -> service.lockOrganization(1L, true)).isInstanceOf(AppException.class);
        assertThat(service.lockOrganization(1L, false).getId()).isEqualTo(1L);
    }

    @Test
    void shouldAllowEnabledManager() {
        when(members.selectOne(any())).thenReturn(OrganizationMember.builder().userId(2L).orgRole("ORG_ADMIN").status(Status.ENABLED).build());
        when(users.selectById(2L)).thenReturn(User.builder().status(Status.ENABLED).build());
        assertThat(service.requireManager(1L, 2L).getUserId()).isEqualTo(2L);
    }

    @Test
    void shouldRejectOrdinaryOrDisabledMemberAndGloballyDisabledManager() {
        when(users.selectById(2L)).thenReturn(User.builder().status(Status.ENABLED).build());
        OrganizationMember member = OrganizationMember.builder().orgRole("ORG_USER").status(Status.ENABLED).build();
        when(members.selectOne(any())).thenReturn(member);
        assertThatThrownBy(() -> service.requireManager(1L, 2L)).isInstanceOf(AppException.class);
        member.setOrgRole("ORG_ADMIN");
        member.setStatus(Status.DISABLED);
        assertThatThrownBy(() -> service.requireManager(1L, 2L)).isInstanceOf(AppException.class);
        member.setStatus(Status.ENABLED);
        when(users.selectById(2L)).thenReturn(User.builder().status(Status.DISABLED).build());
        assertThatThrownBy(() -> service.requireManager(1L, 2L)).isInstanceOf(AppException.class);
    }

    /** 无数据库所有者约束时，缺失、重复或禁用所有者均阻止治理写入。 */
    @Test
    void shouldRejectInvalidOwnerState() {
        when(members.selectOne(any())).thenReturn(OrganizationMember.builder()
                .userId(2L).orgRole("ORG_ADMIN").status(Status.ENABLED).build());
        when(users.selectById(2L)).thenReturn(User.builder().status(Status.ENABLED).build());
        OrganizationMember enabled = OrganizationMember.builder().orgRole("ORG_OWNER").status(Status.ENABLED).build();
        OrganizationMember disabled = OrganizationMember.builder().orgRole("ORG_OWNER").status(Status.DISABLED).build();
        OrganizationMember invalid = OrganizationMember.builder().orgRole("org_owner").status(Status.ENABLED).build();
        for (List<OrganizationMember> owners : List.of(List.<OrganizationMember>of(), List.of(disabled),
                List.of(enabled, enabled), List.of(enabled, disabled), List.of(invalid))) {
            when(members.selectList(any())).thenReturn(owners);
            assertThatThrownBy(() -> service.requireManager(1L, 2L)).isInstanceOf(AppException.class)
                    .extracting("code").isEqualTo(online.longlian.app.common.result.ResultCode.OPERATION_FAIL.getCode());
        }
    }
}
