package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.LockService;
import online.longlian.common.enumeration.ApplicationStatus;
import online.longlian.common.enumeration.ApplicationType;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationReviewHandlerTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private OrganizationMemberMapper organizationMemberMapper;
    @Mock
    private GroupApplicationMapper groupApplicationMapper;
    @Mock
    private OrganizationJoinOtpMapper organizationJoinOtpMapper;
    @Mock
    private LockService lockService;
    @Mock
    private DistributedLockService.Lock lock;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
    private ApplicationReviewHandler handler;

    @BeforeEach
    void setUp() {
        MybatisConfiguration config = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), GroupApplication.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), OrganizationMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), OrganizationJoinOtp.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), User.class);
        handler = new ApplicationReviewHandler(userMapper, organizationMemberMapper,
                groupApplicationMapper, organizationJoinOtpMapper, lockService, clock);
    }

    @Test
    void validatePendingApplication_nullApplication_throws() {
        assertThatThrownBy(() -> handler.validatePendingApplication(null, 1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("入组申请不存在");
    }

    @Test
    void validatePendingApplication_wrongOrg_throws() {
        GroupApplication app = GroupApplication.builder().id(1L).orgId(2L).status(ApplicationStatus.PENDING).build();
        assertThatThrownBy(() -> handler.validatePendingApplication(app, 99L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("入组申请不存在");
    }

    @Test
    void validatePendingApplication_alreadyReviewed_throws() {
        GroupApplication app = GroupApplication.builder().id(1L).orgId(1L).status(ApplicationStatus.APPROVED).build();
        assertThatThrownBy(() -> handler.validatePendingApplication(app, 1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("该申请已审核");
    }

    @Test
    void validatePendingApplication_valid_noException() {
        GroupApplication app = GroupApplication.builder().id(1L).orgId(1L).status(ApplicationStatus.PENDING).build();
        handler.validatePendingApplication(app, 1L);
    }

    @Test
    void approveApplication_existingUser_createsMember() {
        GroupApplication app = GroupApplication.builder()
                .id(1L).orgId(10L).userId(5L)
                .applicationType(ApplicationType.EXISTING_USER)
                .build();
        User user = User.builder().id(5L).status(Status.ENABLED).build();
        when(userMapper.selectById(5L)).thenReturn(user);
        when(organizationMemberMapper.selectOne(any())).thenReturn(null);
        when(organizationMemberMapper.insert(any(OrganizationMember.class))).thenReturn(1);

        OrganizationMember member = handler.approveApplication(app);

        assertThat(member.getOrgId()).isEqualTo(10L);
        assertThat(member.getUserId()).isEqualTo(5L);
        assertThat(member.getStatus()).isEqualTo(Status.ENABLED);
    }

    @Test
    void approveApplication_registerType_createsUserAndMember() {
        GroupApplication app = GroupApplication.builder()
                .id(1L).orgId(10L)
                .applicationType(ApplicationType.REGISTER)
                .email("test@example.com").username("testuser")
                .password("hashed").nickname("Test")
                .build();
        when(lockService.tryAcquireOrThrow(anyString(), eq(0L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(lock);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(organizationMemberMapper.insert(any(OrganizationMember.class))).thenReturn(1);

        OrganizationMember member = handler.approveApplication(app);

        assertThat(member.getOrgId()).isEqualTo(10L);
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void createUserByApplication_emailExists_throws() {
        GroupApplication app = GroupApplication.builder()
                .email("dup@example.com").username("user1").build();
        when(lockService.tryAcquireOrThrow(anyString(), eq(0L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(lock);
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> handler.createUserByApplication(app, LocalDateTime.now(clock)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邮箱已存在");
    }

    @Test
    void createUserByApplication_usernameExists_throws() {
        GroupApplication app = GroupApplication.builder()
                .email("new@example.com").username("dupuser").build();
        when(lockService.tryAcquireOrThrow(anyString(), eq(0L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(lock);
        when(userMapper.selectCount(any())).thenReturn(0L).thenReturn(1L);

        assertThatThrownBy(() -> handler.createUserByApplication(app, LocalDateTime.now(clock)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void getExistingApplicationUser_userNotFound_throws() {
        GroupApplication app = GroupApplication.builder().userId(99L).orgId(1L).build();
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> handler.getExistingApplicationUser(app))
                .isInstanceOf(AppException.class);
    }

    @Test
    void getExistingApplicationUser_userDisabled_throws() {
        GroupApplication app = GroupApplication.builder().userId(5L).orgId(1L).build();
        when(userMapper.selectById(5L)).thenReturn(User.builder().id(5L).status(Status.DISABLED).build());

        assertThatThrownBy(() -> handler.getExistingApplicationUser(app))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("申请人已被禁用");
    }

    @Test
    void getExistingApplicationUser_alreadyMember_throws() {
        GroupApplication app = GroupApplication.builder().userId(5L).orgId(1L).build();
        when(userMapper.selectById(5L)).thenReturn(User.builder().id(5L).status(Status.ENABLED).build());
        when(organizationMemberMapper.selectOne(any())).thenReturn(OrganizationMember.builder().build());

        assertThatThrownBy(() -> handler.getExistingApplicationUser(app))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("申请人已加入该组织");
    }

    @Test
    void rejectApplication_null_throws() {
        assertThatThrownBy(() -> handler.rejectApplication(null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("入组申请不存在");
    }

    @Test
    void review_approved_fullFlow() {
        GroupApplication app = GroupApplication.builder()
                .id(1L).orgId(10L).userId(5L)
                .applicationType(ApplicationType.EXISTING_USER)
                .status(ApplicationStatus.PENDING)
                .build();
        User user = User.builder().id(5L).status(Status.ENABLED).build();
        when(userMapper.selectById(5L)).thenReturn(user);
        when(organizationMemberMapper.selectOne(any())).thenReturn(null);
        when(organizationMemberMapper.insert(any(OrganizationMember.class))).thenReturn(1);

        Page<OrganizationJoinOtp> emptyPage = new Page<>(1, 1);
        emptyPage.setRecords(List.of());
        when(organizationJoinOtpMapper.selectPage(any(), any())).thenReturn(emptyPage);

        handler.review(app, 10L, ApplicationStatus.APPROVED, 2L, "ok", LocalDateTime.now(clock));

        verify(groupApplicationMapper).update(isNull(), any());
    }

    @Test
    void review_rejected_updatesStatus() {
        GroupApplication app = GroupApplication.builder()
                .id(1L).orgId(10L)
                .applicationType(ApplicationType.EXISTING_USER)
                .status(ApplicationStatus.PENDING)
                .build();

        handler.review(app, 10L, ApplicationStatus.REJECTED, 2L, "不符合", LocalDateTime.now(clock));

        verify(groupApplicationMapper).update(isNull(), any());
        verify(organizationMemberMapper, never()).insert(any(OrganizationMember.class));
    }

    @Test
    void review_approved_backfillsOtpForExistingUser() {
        GroupApplication app = GroupApplication.builder()
                .id(1L).orgId(10L).userId(5L)
                .applicationType(ApplicationType.EXISTING_USER)
                .status(ApplicationStatus.PENDING)
                .build();
        User user = User.builder().id(5L).status(Status.ENABLED).build();
        when(userMapper.selectById(5L)).thenReturn(user);
        when(organizationMemberMapper.selectOne(any())).thenReturn(null);
        when(organizationMemberMapper.insert(any(OrganizationMember.class))).thenReturn(1);

        OrganizationJoinOtp otp = OrganizationJoinOtp.builder().id(99L).build();
        Page<OrganizationJoinOtp> page = new Page<>(1, 1);
        page.setRecords(List.of(otp));
        when(organizationJoinOtpMapper.selectPage(any(), any())).thenReturn(page);

        handler.review(app, 10L, ApplicationStatus.APPROVED, 2L, null, LocalDateTime.now(clock));

        verify(organizationJoinOtpMapper).update(isNull(), any());
    }
}
