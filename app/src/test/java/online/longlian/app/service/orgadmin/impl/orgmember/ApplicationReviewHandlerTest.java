package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.*;
import online.longlian.app.pojo.entity.*;
import online.longlian.common.enumeration.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationReviewHandlerTest {
    @Mock private UserMapper users;
    @Mock private OrganizationMemberMapper members;
    @Mock private GroupApplicationMapper applications;
    @Mock private OrganizationJoinOtpMapper otps;
    private final LocalDateTime reviewedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
    private ApplicationReviewHandler handler;

    @BeforeEach
    void setUp() {
        for (Class<?> entity : List.of(User.class, OrganizationMember.class, GroupApplication.class, OrganizationJoinOtp.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entity);
        }
        handler = new ApplicationReviewHandler(users, members, applications, otps);
    }

    /** 注册审批创建正式账号和成员，并使用统一的审核时间。 */
    @Test
    void shouldCreateEnabledUserAndMemberFromRegistrationSnapshot() {
        when(users.insert(any(User.class))).thenAnswer(call -> {
            call.getArgument(0, User.class).setId(5L);
            return 1;
        });
        when(applications.update(isNull(), any())).thenReturn(1);

        approve(registration());

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<OrganizationMember> member = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(users).insert(user.capture());
        verify(members).insert(member.capture());
        assertThat(user.getValue().getUsername()).isEqualTo("newuser");
        assertThat(user.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(user.getValue().getStatus()).isEqualTo(Status.ENABLED);
        assertThat(user.getValue().getDefaultOrgId()).isEqualTo(10L);
        assertThat(user.getValue().getPassword()).isEqualTo("hash");
        assertThat(user.getValue().getCreatedAt()).isEqualTo(reviewedAt);
        assertThat(user.getValue().getUpdatedAt()).isEqualTo(reviewedAt);
        assertThat(member.getValue().getUserId()).isEqualTo(5L);
        assertThat(member.getValue().getOrgId()).isEqualTo(10L);
        assertThat(member.getValue().getOrgRole()).isEqualTo("ORG_USER");
        assertThat(member.getValue().getStatus()).isEqualTo(Status.ENABLED);
        assertThat(member.getValue().getJoinedAt()).isEqualTo(reviewedAt);
        assertThat(member.getValue().getCreatedAt()).isEqualTo(reviewedAt);
        assertThat(member.getValue().getUpdatedAt()).isEqualTo(reviewedAt);
    }

    /** 身份唯一键冲突不得继续创建成员或更新申请。 */
    @Test
    void shouldStopApprovalWhenIdentityInsertConflicts() {
        when(users.insert(any(User.class))).thenThrow(new DuplicateKeyException("identity"));

        assertThatThrownBy(() -> approve(registration())).isInstanceOf(DuplicateKeyException.class);
        verifyNoInteractions(members, applications, otps);
    }

    /** 未转换的历史占位账号不能按新快照流程审批。 */
    @Test
    void shouldRejectUnconvertedLegacyRegistration() {
        GroupApplication application = registration();
        application.setUserId(5L);

        assertThatThrownBy(() -> approve(application)).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(users, members, applications, otps);
    }

    /** 缺少密码快照时不能创建正式账号。 */
    @Test
    void shouldRejectRegistrationWithoutPasswordSnapshot() {
        GroupApplication application = registration();
        application.setPasswordHash(null);

        assertThatThrownBy(() -> approve(application)).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(users, members, applications, otps);
    }

    /** 拒绝只终结申请，不创建或删除账号和成员。 */
    @Test
    void shouldRejectRegistrationWithoutChangingAnyUser() {
        when(applications.update(isNull(), any())).thenReturn(1);

        handler.review(registration(), 10L, ApplicationStatus.REJECTED, 2L, "", reviewedAt);

        verifyNoInteractions(users, members, otps);
    }

    /** 条件更新未命中时审核必须失败。 */
    @Test
    void shouldFailWhenApplicationIsNoLongerPending() {
        assertThatThrownBy(() -> handler.review(registration(), 10L, ApplicationStatus.REJECTED, 2L, "", reviewedAt))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
    }

    /** 已有用户审批只建立成员关系，不新建账号。 */
    @Test
    void shouldApproveExistingUserWithoutCreatingAccount() {
        when(users.selectById(5L)).thenReturn(User.builder().id(5L).status(Status.ENABLED).build());
        when(applications.update(isNull(), any())).thenReturn(1);

        approve(existingUserApplication());

        verify(users, never()).insert(any(User.class));
        ArgumentCaptor<OrganizationMember> member = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(members).insert(member.capture());
        assertThat(member.getValue().getUserId()).isEqualTo(5L);
        assertThat(member.getValue().getOrgId()).isEqualTo(10L);
    }

    /** 申请人被禁用后不能批准其加入组织。 */
    @Test
    void shouldRejectDisabledExistingAccount() {
        when(users.selectById(5L)).thenReturn(User.builder().id(5L).status(Status.DISABLED).build());

        assertThatThrownBy(() -> approve(existingUserApplication())).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(members, applications, otps);
    }

    /** 申请人不存在时不能建立成员关系。 */
    @Test
    void shouldRejectMissingExistingAccount() {
        assertThatThrownBy(() -> approve(existingUserApplication())).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.USER_NOT_EXIT.getCode());
        verifyNoInteractions(members, applications, otps);
    }

    /** 非本组织或不存在的申请不得被审核，已处理的申请不得再次审核。 */
    @Test
    void shouldRejectForeignMissingOrAlreadyReviewedApplication() {
        GroupApplication application = registration();
        assertThatThrownBy(() -> handler.review(application, 11L, ApplicationStatus.APPROVED, 2L, "", reviewedAt))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        application.setStatus(ApplicationStatus.APPROVED);
        assertThatThrownBy(() -> approve(application)).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        assertThatThrownBy(() -> approve(null)).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        verifyNoInteractions(users, members, applications, otps);
    }

    private void approve(GroupApplication application) {
        handler.review(application, 10L, ApplicationStatus.APPROVED, 2L, "", reviewedAt);
    }

    /** 成员唯一性由组织锁内的应用查询保证，禁用成员也不能重复创建。 */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(Status.class)
    void shouldRejectApprovalForExistingMembership(Status status) {
        when(users.selectById(5L)).thenReturn(User.builder().id(5L).status(Status.ENABLED).build());
        when(members.selectOne(any())).thenReturn(OrganizationMember.builder().id(9L).status(status).build());
        assertThatThrownBy(() -> approve(existingUserApplication())).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verify(members, never()).insert(any(OrganizationMember.class));
        verifyNoInteractions(applications, otps);
    }

    /** 数据库不设 CHECK 时，缺少账号引用的已有用户申请仍不能写入成员。 */
    @Test
    void shouldRejectExistingApplicationWithoutUserReference() {
        GroupApplication application = existingUserApplication();
        application.setUserId(null);
        assertThatThrownBy(() -> approve(application)).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(users, members, applications, otps);
    }

    /** 已有用户申请不能携带注册密码快照。 */
    @Test
    void shouldRejectPasswordSnapshotOnExistingApplication() {
        GroupApplication application = existingUserApplication();
        application.setPasswordHash("hash");
        assertThatThrownBy(() -> approve(application)).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(users, members, applications, otps);
    }

    /** 历史缺失或未知的申请类型不能进入审批写入流程。 */
    @Test
    void shouldRejectUnknownApplicationType() {
        GroupApplication application = registration();
        application.setApplicationType(null);
        assertThatThrownBy(() -> approve(application)).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(users, members, applications, otps);
    }

    private GroupApplication existingUserApplication() {
        GroupApplication application = registration();
        application.setApplicationType(ApplicationType.EXISTING_USER);
        application.setUserId(5L);
        application.setPasswordHash(null);
        return application;
    }

    private GroupApplication registration() {
        return GroupApplication.builder().id(1L).orgId(10L).status(ApplicationStatus.PENDING)
                .applicationType(ApplicationType.REGISTER).username("newuser").email("new@example.com")
                .nickname("New").passwordHash("hash").build();
    }
}
