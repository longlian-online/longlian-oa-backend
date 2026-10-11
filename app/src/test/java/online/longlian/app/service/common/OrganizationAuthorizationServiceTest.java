package online.longlian.app.service.common;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationAuthorizationServiceTest {
    @Mock private OrganizationMapper organizations;
    @Mock private OrganizationMemberMapper members;
    @Mock private UserMapper users;
    private OrganizationAuthorizationService authorization;

    @BeforeEach
    void setUp() {
        for (Class<?> type : List.of(Organization.class, OrganizationMember.class, User.class))
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), type);
        authorization = new OrganizationAuthorizationService(organizations, members, users);
    }

    /** 所有者和普通管理员均可管理组织，所有者额外获得转让与解散资格。 */
    @ParameterizedTest
    @ValueSource(strings = {"ORG_OWNER", "ORG_ADMIN"})
    void shouldAuthorizeEnabledManagers(String role) {
        when(members.selectOne(any())).thenReturn(member(role, Status.ENABLED));
        when(users.selectById(1L)).thenReturn(User.builder().status(Status.ENABLED).build());
        assertThat(authorization.requireManager(1L, 1L).getOrgRole()).isEqualTo(role);
        if (role.equals("ORG_OWNER")) assertThat(authorization.requireOwner(1L, 1L).getUserId()).isEqualTo(1L);
        else assertThatThrownBy(() -> authorization.requireOwner(1L, 1L))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
    }

    /** 普通成员、禁用关系和全局禁用账号都不能取得管理权限。 */
    @Test
    void shouldRejectOrdinaryOrDisabledManagers() {
        when(users.selectById(1L)).thenReturn(User.builder().status(Status.ENABLED).build());
        when(members.selectOne(any())).thenReturn(member("ORG_USER", Status.ENABLED), member("ORG_ADMIN", Status.DISABLED));
        for (int attempt = 0; attempt < 2; attempt++)
            assertThatThrownBy(() -> authorization.requireManager(1L, 1L))
                    .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        when(members.selectOne(any())).thenReturn(member("ORG_OWNER", Status.ENABLED));
        when(users.selectById(1L)).thenReturn(User.builder().status(Status.DISABLED).build());
        assertThatThrownBy(() -> authorization.requireOwner(1L, 1L))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
    }

    /** 成员关系或账号已删除时，不允许继续治理。 */
    @Test
    void shouldRejectMissingOperatorOrAccount() {
        when(users.selectById(1L)).thenReturn(User.builder().status(Status.ENABLED).build());
        assertThatThrownBy(() -> authorization.requireManager(1L, 1L))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        when(members.selectOne(any())).thenReturn(member("ORG_OWNER", Status.ENABLED));
        when(users.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> authorization.requireOwner(1L, 1L))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
    }

    /** 被禁用组织不能治理，但成员仍能使用退出流程。 */
    @Test
    void shouldAllowExitButRejectGovernanceForDisabledOrganization() {
        when(organizations.selectOne(any())).thenReturn(Organization.builder().id(1L).status(Status.DISABLED).build());
        assertThatThrownBy(() -> authorization.lockOrganization(1L, true))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        assertThat(authorization.lockOrganization(1L, false).getStatus()).isEqualTo(Status.DISABLED);
    }

    /** 已解散组织不允许通过退出或治理入口重新写入。 */
    @Test
    void shouldRejectDeletedOrganization() {
        assertThatThrownBy(() -> authorization.lockOrganization(1L, false))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
    }

    private OrganizationMember member(String role, Status status) {
        return OrganizationMember.builder().id(1L).orgId(1L).userId(1L).orgRole(role).status(status).build();
    }
}
