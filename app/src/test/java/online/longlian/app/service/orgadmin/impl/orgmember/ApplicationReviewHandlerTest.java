package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.*;
import online.longlian.app.pojo.entity.*;
import online.longlian.common.enumeration.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
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
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private ApplicationReviewHandler handler;

    @BeforeEach
    void setUp() {
        for (Class<?> entity : List.of(User.class, OrganizationMember.class, GroupApplication.class, OrganizationJoinOtp.class))
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entity);
        handler = new ApplicationReviewHandler(users, members, applications, otps, clock);
    }

    @Test
    void shouldCreateEnabledUserFromRegistrationSnapshot() {
        GroupApplication application = registration();
        when(users.insert(any(User.class))).thenAnswer(call -> {call.getArgument(0, User.class).setId(5L); return 1;});
        OrganizationMember member = handler.approveApplication(application);
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(users).insert(user.capture());
        assertThat(member.getUserId()).isEqualTo(5L);
        assertThat(user.getValue().getStatus()).isEqualTo(Status.ENABLED);
        assertThat(user.getValue().getDefaultOrgId()).isEqualTo(10L);
        assertThat(user.getValue().getPassword()).isEqualTo("hash");
    }

    @Test
    void shouldKeepIdentityConflictPending() {
        when(users.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> handler.approveApplication(registration())).isInstanceOf(AppException.class);
        verify(users, never()).insert(any(User.class));
        verifyNoInteractions(members, applications);
    }

    @Test
    void shouldRejectUnconvertedLegacyRegistration() {
        GroupApplication application = registration();
        application.setUserId(5L);
        assertThatThrownBy(() -> handler.approveApplication(application)).isInstanceOf(AppException.class);
        verifyNoInteractions(users, members);
    }

    @Test
    void shouldRejectRegistrationWithoutPasswordSnapshot() {
        GroupApplication application = registration();
        application.setPasswordHash(null);
        assertThatThrownBy(() -> handler.approveApplication(application)).isInstanceOf(AppException.class);
    }

    @Test
    void shouldRejectRegistrationWithoutDeletingAnyUser() {
        when(applications.update(isNull(), any())).thenReturn(1);
        handler.review(registration(),10L,ApplicationStatus.REJECTED,2L,"",LocalDateTime.now(clock));
        verifyNoInteractions(users, members);
        verify(applications).update(isNull(), argThat(wrapper ->
                ((com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<GroupApplication>)wrapper).getSqlSet().contains("password_hash")));
    }

    @Test
    void shouldRollbackWhenConditionalApplicationUpdateLosesRace() {
        assertThatThrownBy(() -> handler.review(registration(),10L,ApplicationStatus.REJECTED,2L,"",LocalDateTime.now(clock)))
                .isInstanceOf(AppException.class).hasMessageContaining("已审核");
    }

    @Test
    void shouldApproveExistingUserWithoutCreatingAccount() {
        GroupApplication application = registration();
        application.setApplicationType(ApplicationType.EXISTING_USER);
        application.setUserId(5L);
        when(users.selectById(5L)).thenReturn(User.builder().id(5L).status(Status.ENABLED).build());
        handler.approveApplication(application);
        verify(users, never()).insert(any(User.class));
        verify(members).insert(any(OrganizationMember.class));
    }

    @Test
    void shouldRejectDisabledExistingAccount() {
        GroupApplication application = registration();
        application.setUserId(5L);
        when(users.selectById(5L)).thenReturn(User.builder().id(5L).status(Status.DISABLED).build());
        assertThatThrownBy(() -> handler.getExistingApplicationUser(application)).isInstanceOf(AppException.class);
    }

    @Test
    void shouldRejectForeignOrAlreadyReviewedApplication() {
        GroupApplication application = registration();
        assertThatThrownBy(() -> handler.validatePendingApplication(application,11L)).isInstanceOf(AppException.class);
        application.setStatus(ApplicationStatus.APPROVED);
        assertThatThrownBy(() -> handler.validatePendingApplication(application,10L)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> handler.validatePendingApplication(null,10L)).isInstanceOf(AppException.class);
    }

    private GroupApplication registration() {
        return GroupApplication.builder().id(1L).orgId(10L).status(ApplicationStatus.PENDING)
                .applicationType(ApplicationType.REGISTER).username("newuser").email("new@example.com")
                .nickname("New").passwordHash("hash").build();
    }
}
