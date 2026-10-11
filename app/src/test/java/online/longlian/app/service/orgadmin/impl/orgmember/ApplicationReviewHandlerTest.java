package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.ApplicationStatus;
import online.longlian.common.enumeration.ApplicationType;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationReviewHandlerTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 11, 12, 0);
    @Mock private UserMapper users;
    @Mock private OrganizationMemberMapper members;
    @Mock private GroupApplicationMapper applications;
    @Mock private OrganizationJoinOtpMapper invites;
    private ApplicationReviewHandler handler;

    @BeforeEach
    void setUp() {
        for (Class<?> entity : List.of(User.class, OrganizationMember.class, GroupApplication.class, OrganizationJoinOtp.class))
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entity);
        handler = new ApplicationReviewHandler(users, members, applications, invites);
    }

    /** 注册审批仍启用已有禁用账号，不把快照模型混入组织治理。 */
    @Test
    void shouldActivatePendingRegisteredUserAndCreateOrdinaryMembership() {
        User pending = User.builder().id(5L).status(Status.DISABLED).defaultOrgId(0L).build();
        when(users.selectOne(any())).thenReturn(pending);
        handler.review(application(ApplicationType.REGISTER), 1L, ApplicationStatus.APPROVED, 2L, null, NOW);
        assertThat(pending.getStatus()).isEqualTo(Status.ENABLED);
        assertThat(pending.getDefaultOrgId()).isEqualTo(1L);
        ArgumentCaptor<OrganizationMember> created = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(members).insert(created.capture());
        assertThat(created.getValue().getOrgRole()).isEqualTo("ORG_USER");
        assertThat(created.getValue().getOrgId()).isEqualTo(1L);
        assertThat(created.getValue().getUserId()).isEqualTo(5L);
    }

    /** 已有用户入组不覆盖全局状态或其他默认组织，也不能继承管理角色。 */
    @Test
    void shouldCreateOrdinaryMembershipWithoutChangingExistingUser() {
        User user = User.builder().id(5L).status(Status.ENABLED).defaultOrgId(9L).build();
        when(users.selectOne(any())).thenReturn(user);
        handler.review(application(ApplicationType.EXISTING_USER), 1L, ApplicationStatus.APPROVED, 2L, "", NOW);
        ArgumentCaptor<OrganizationMember> created = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(members).insert(created.capture());
        assertThat(created.getValue().getOrgRole()).isEqualTo("ORG_USER");
        assertThat(user.getDefaultOrgId()).isEqualTo(9L);
        verify(users, never()).updateById(any(User.class));
    }

    /** 拒绝申请不得启用账号或创建成员。 */
    @Test
    void shouldRejectApplicationWithoutActivatingAccountOrMembership() {
        handler.review(application(ApplicationType.REGISTER), 1L, ApplicationStatus.REJECTED, 2L, "reason", NOW);
        verifyNoInteractions(users, members, invites);
    }

    /** 不存在、跨组织或已审核申请都不能再次产生账号或成员副作用。 */
    @Test
    void shouldRejectMissingForeignAndReviewedApplications() {
        assertThatThrownBy(() -> handler.review(null, 1L, ApplicationStatus.APPROVED, 2L, null, NOW))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        assertThatThrownBy(() -> handler.review(application(ApplicationType.EXISTING_USER), 9L,
                ApplicationStatus.APPROVED, 2L, null, NOW))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        GroupApplication reviewed = application(ApplicationType.EXISTING_USER);
        reviewed.setStatus(ApplicationStatus.REJECTED);
        assertThatThrownBy(() -> handler.review(reviewed, 1L, ApplicationStatus.APPROVED, 2L, null, NOW))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(users, members, applications, invites);
    }

    /** 禁用成员也属于已有关系，审批不得悄悄创建新的启用成员绕过禁用。 */
    @Test
    void shouldRejectDuplicateDisabledMembership() {
        when(users.selectOne(any())).thenReturn(User.builder().id(5L).status(Status.ENABLED).build());
        when(members.selectOne(any())).thenReturn(OrganizationMember.builder().status(Status.DISABLED).build());
        assertThatThrownBy(() -> handler.review(application(ApplicationType.EXISTING_USER), 1L,
                ApplicationStatus.APPROVED, 2L, null, NOW))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verify(members, never()).insert(any(OrganizationMember.class));
        verifyNoInteractions(applications, invites);
    }

    /** 注册申请不能再次启用已启用账号，已有用户申请不能启用全局禁用账号。 */
    @Test
    void shouldRejectIncompatibleApplicationUserStatus() {
        when(users.selectOne(any())).thenReturn(User.builder().id(5L).status(Status.ENABLED).build());
        assertThatThrownBy(() -> handler.review(application(ApplicationType.REGISTER), 1L,
                ApplicationStatus.APPROVED, 2L, null, NOW))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        when(users.selectOne(any())).thenReturn(User.builder().id(5L).status(Status.DISABLED).build());
        assertThatThrownBy(() -> handler.review(application(ApplicationType.EXISTING_USER), 1L,
                ApplicationStatus.APPROVED, 2L, null, NOW))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(members, applications, invites);
    }

    /** 账号已经删除时不能建立悬空成员关系。 */
    @Test
    void shouldRejectApplicationForMissingUser() {
        assertThatThrownBy(() -> handler.review(application(ApplicationType.EXISTING_USER), 1L,
                ApplicationStatus.APPROVED, 2L, null, NOW))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.USER_NOT_EXIT.getCode());
        verifyNoInteractions(members, applications, invites);
    }

    /** 历史申请缺少类型时不能被当作已有账号审批。 */
    @Test
    void shouldRejectApplicationWithoutType() {
        when(users.selectOne(any())).thenReturn(User.builder().id(5L).status(Status.ENABLED).build());
        assertThatThrownBy(() -> handler.review(application(null), 1L, ApplicationStatus.APPROVED, 2L, null, NOW))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(members, applications, invites);
    }

    private GroupApplication application(ApplicationType type) {
        return GroupApplication.builder().id(10L).orgId(1L).userId(5L)
                .status(ApplicationStatus.PENDING).applicationType(type).build();
    }
}
