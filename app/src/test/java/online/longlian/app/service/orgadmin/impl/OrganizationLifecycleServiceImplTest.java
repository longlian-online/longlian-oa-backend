package online.longlian.app.service.orgadmin.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberExitParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberRemoveParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberTransferOwnershipParamsBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.OrganizationAuthorizationService;
import online.longlian.app.service.orgadmin.impl.orgmember.OrganizationMemberPolicy;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationLifecycleServiceImplTest {
    @Mock private OrganizationMapper organizations;
    @Mock private OrganizationMemberMapper members;
    @Mock private UserMapper users;
    @Mock private GroupApplicationMapper applications;
    private OrganizationLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        for (Class<?> type : List.of(Organization.class, OrganizationMember.class, User.class, GroupApplication.class))
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), type);
        OrganizationAuthorizationService authorization = new OrganizationAuthorizationService(organizations, members, users);
        service = new OrganizationLifecycleServiceImpl(organizations, members, users, applications,
                authorization, new OrganizationMemberPolicy(), Clock.systemUTC());
        lenient().when(organizations.selectOne(any())).thenReturn(Organization.builder().status(Status.ENABLED).build());
        lenient().when(users.selectById(1L)).thenReturn(User.builder().status(Status.ENABLED).build());
    }

    /** 所有者退出前必须转让，不能删除当前唯一所有者。 */
    @Test
    void shouldRejectOwnerExitBeforeDeletingMembership() {
        when(members.selectOne(any())).thenReturn(member(1L, "ORG_OWNER", Status.ENABLED));
        assertThatThrownBy(() -> service.exitOrganization(exitParams()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verify(members, never()).deleteById(any(Long.class));
    }

    /** 禁用关系的成员仍可退出，避免把退出误写成只能由启用成员执行。 */
    @Test
    void shouldAllowDisabledOrdinaryMemberToExit() {
        when(members.selectOne(any())).thenReturn(member(1L, "ORG_USER", Status.DISABLED));
        service.exitOrganization(exitParams());
        verify(members).deleteById(1L);
        verify(users, never()).updateById(any(User.class));
    }

    /** 不存在的关系不能作为成功退组处理。 */
    @Test
    void shouldRejectMissingMembershipOnExit() {
        assertThatThrownBy(() -> service.exitOrganization(exitParams()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        verify(members, never()).deleteById(any(Long.class));
    }

    /** 普通管理员不能移除同级管理员，不能通过生命周期入口绕过状态治理策略。 */
    @Test
    void shouldRejectRemovingPeerAdministrator() {
        when(members.selectOne(any())).thenReturn(member(1L, "ORG_ADMIN", Status.ENABLED), member(2L, "ORG_ADMIN", Status.ENABLED));
        assertThatThrownBy(() -> service.removeMember(OrgMemberRemoveParamsBO.builder()
                .orgId(1L).operatorUserId(1L).memberId(2L).build()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        verify(members, never()).deleteById(any(Long.class));
    }

    /** 转让给全局禁用账号之前拒绝，不得先降级原所有者。 */
    @Test
    void shouldRejectOwnershipTransferToUnavailableAccount() {
        when(members.selectOne(any())).thenReturn(member(1L, "ORG_OWNER", Status.ENABLED), member(2L, "ORG_USER", Status.ENABLED));
        when(users.selectOne(any())).thenReturn(User.builder().status(Status.DISABLED).build());
        assertThatThrownBy(() -> service.transferOwnership(transferParams()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verify(members, never()).update(isNull(), any());
    }

    /** 目标成员已被移除时，不允许把原所有者降级。 */
    @Test
    void shouldRejectTransferToMissingMemberBeforeChangingOwner() {
        when(members.selectOne(any())).thenReturn(member(1L, "ORG_OWNER", Status.ENABLED), null);
        assertThatThrownBy(() -> service.transferOwnership(transferParams()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        verify(members, never()).update(isNull(), any());
    }

    private OrganizationMember member(Long id, String role, Status status) {
        return OrganizationMember.builder().id(id).orgId(1L).userId(id).orgRole(role).status(status).build();
    }

    private OrgMemberExitParamsBO exitParams() {
        return OrgMemberExitParamsBO.builder().orgId(1L).operatorUserId(1L).build();
    }

    private OrgMemberTransferOwnershipParamsBO transferParams() {
        return OrgMemberTransferOwnershipParamsBO.builder().orgId(1L).operatorUserId(1L).memberId(2L).build();
    }
}
