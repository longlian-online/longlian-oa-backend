package online.longlian.app.service.invite.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationInviteCodeMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.invite.OrganizationInviteCodeAcceptParamsBO;
import online.longlian.app.pojo.bo.invite.OrganizationInviteCodeGenerateParamsBO;
import online.longlian.app.pojo.bo.invite.OrganizationInviteCodeGenerateResultBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationInviteCode;
import online.longlian.common.enumeration.OTPType;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationInviteCodeServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock
    private OrganizationInviteCodeMapper organizationInviteCodeMapper;
    @Mock
    private OrganizationMapper organizationMapper;

    private OrganizationInviteCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationInviteCode.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Organization.class);
        service = new OrganizationInviteCodeServiceImpl(organizationInviteCodeMapper, organizationMapper, CLOCK);
    }

    @Test
    void shouldPersistChosenExpiryAndLeaveInviteeUnaccepted() {
        when(organizationInviteCodeMapper.selectCount(any())).thenReturn(0L);
        when(organizationInviteCodeMapper.insert(any(OrganizationInviteCode.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, OrganizationInviteCode.class).setId(8L);
            return 1;
        });
        LocalDateTime expiredAt = LocalDateTime.now(CLOCK).plusDays(2);

        OrganizationInviteCodeGenerateResultBO result = service.generate(OrganizationInviteCodeGenerateParamsBO.builder()
                .bizType(OTPType.OrganizationInvite)
                .creatorId(3L)
                .expiredAt(expiredAt)
                .build());

        ArgumentCaptor<OrganizationInviteCode> captor = ArgumentCaptor.forClass(OrganizationInviteCode.class);
        verify(organizationInviteCodeMapper).insert(captor.capture());
        OrganizationInviteCode saved = captor.getValue();
        assertThat(result.getId()).isEqualTo(8L);
        assertThat(result.getInviteCode()).hasSize(6);
        assertThat(saved.getCode()).isEqualTo(result.getInviteCode());
        assertThat(saved.getExpiredAt()).isEqualTo(expiredAt);
        assertThat(saved.getCreatorId()).isEqualTo(3L);
        assertThat(saved.getInviteeUserId()).isNull();
        assertThat(saved.getAccepted()).isZero();
        assertThat(saved.getAcceptedAt()).isNull();
        assertThat(saved.getOrgId()).isNull();
        assertThat(saved.getBizType()).isEqualTo(OTPType.OrganizationInvite);
    }

    @Test
    void shouldRejectExpiryThatIsNotInTheFuture() {
        assertThatThrownBy(() -> service.generate(OrganizationInviteCodeGenerateParamsBO.builder()
                .bizType(OTPType.OrganizationUserInvite)
                .creatorId(1L)
                .orgId(2L)
                .expiredAt(LocalDateTime.now(CLOCK))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("到期时间必须晚于当前时间")
                .extracting("code").isEqualTo(ResultCode.PARAM_ERROR.getCode());
        verify(organizationInviteCodeMapper, never()).insert(any(OrganizationInviteCode.class));
    }

    @Test
    void shouldRejectJoinInviteWhenOrganizationIsDisabled() {
        when(organizationMapper.selectById(2L)).thenReturn(Organization.builder().id(2L).status(Status.DISABLED).build());

        assertThatThrownBy(() -> service.generate(OrganizationInviteCodeGenerateParamsBO.builder()
                .bizType(OTPType.OrganizationUserInvite)
                .creatorId(1L)
                .orgId(2L)
                .expiredAt(LocalDateTime.now(CLOCK).plusHours(1))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织不存在或已禁用");
        verify(organizationInviteCodeMapper, never()).insert(any(OrganizationInviteCode.class));
    }

    @Test
    void shouldReturnActiveInviteAndRejectInvalidAcceptedOrExpiredCodes() {
        OrganizationInviteCode active = OrganizationInviteCode.builder().id(1L).code("ABC123").accepted(0)
                .expiredAt(LocalDateTime.now(CLOCK).plusHours(1)).orgId(9L).build();
        when(organizationInviteCodeMapper.selectOne(any())).thenReturn(active);
        assertThat(service.getValid("ABC123", OTPType.OrganizationUserInvite).getOrgId()).isEqualTo(9L);

        when(organizationInviteCodeMapper.selectOne(any())).thenReturn(null, OrganizationInviteCode.builder()
                .id(2L).accepted(1).acceptedAt(LocalDateTime.now(CLOCK).minusMinutes(1)).build());
        assertThatThrownBy(() -> service.getValid("USED01", OTPType.OrganizationInvite))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码已接受");

        when(organizationInviteCodeMapper.selectOne(any())).thenReturn(null, OrganizationInviteCode.builder()
                .id(3L).accepted(0).expiredAt(LocalDateTime.now(CLOCK).minusMinutes(1)).build());
        assertThatThrownBy(() -> service.getValid("OLD001", OTPType.OrganizationInvite))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码已过期");

        when(organizationInviteCodeMapper.selectOne(any())).thenReturn(null, (OrganizationInviteCode) null);
        assertThatThrownBy(() -> service.getValid("NOPE01", OTPType.OrganizationUserInvite))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码无效");
    }

    @Test
    void shouldMarkAcceptedInviteeAndRejectASecondAccept() {
        when(organizationInviteCodeMapper.update(isNull(), any())).thenReturn(1);

        service.accept(OrganizationInviteCodeAcceptParamsBO.builder().id(8L).inviteeUserId(4L).orgId(5L).build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<OrganizationInviteCode>> update = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(organizationInviteCodeMapper).update(isNull(), update.capture());
        assertThat(update.getValue().getSqlSet()).contains("accepted", "accepted_at", "invitee_user_id", "org_id");

        when(organizationInviteCodeMapper.update(isNull(), any())).thenReturn(0);
        when(organizationInviteCodeMapper.selectById(8L)).thenReturn(OrganizationInviteCode.builder()
                .id(8L).accepted(1).acceptedAt(LocalDateTime.now(CLOCK)).build());
        assertThatThrownBy(() -> service.accept(OrganizationInviteCodeAcceptParamsBO.builder().id(8L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码已接受");
    }

    @Test
    void shouldSkipBindingWhenInviteRecordIsMissing() {
        service.bindInvitee(null, 1L, 2L);
        verify(organizationInviteCodeMapper, never()).update(any(), any());
    }

    @Test
    void shouldStoreTargetOrganizationWhenGeneratingJoinInvite() {
        when(organizationMapper.selectById(2L)).thenReturn(Organization.builder().id(2L).status(Status.ENABLED).build());
        when(organizationInviteCodeMapper.selectCount(any())).thenReturn(1L, 0L);
        when(organizationInviteCodeMapper.insert(any(OrganizationInviteCode.class))).thenReturn(1);

        OrganizationInviteCodeGenerateResultBO result = service.generate(OrganizationInviteCodeGenerateParamsBO.builder()
                .bizType(OTPType.OrganizationUserInvite)
                .creatorId(1L)
                .orgId(2L)
                .expiredAt(LocalDateTime.now(CLOCK).plusHours(3))
                .build());

        ArgumentCaptor<OrganizationInviteCode> captor = ArgumentCaptor.forClass(OrganizationInviteCode.class);
        verify(organizationInviteCodeMapper).insert(captor.capture());
        assertThat(captor.getValue().getOrgId()).isEqualTo(2L);
        assertThat(captor.getValue().getBizType()).isEqualTo(OTPType.OrganizationUserInvite);
        assertThat(result.getInviteCode()).isEqualTo(captor.getValue().getCode());
    }

    @Test
    void shouldRejectInvalidInviteTypeMissingOrganizationAndRepeatedCodeCollisions() {
        assertThatThrownBy(() -> service.generate(OrganizationInviteCodeGenerateParamsBO.builder()
                .bizType(OTPType.EmailVerify)
                .creatorId(1L)
                .expiredAt(LocalDateTime.now(CLOCK).plusHours(1))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码类型无效");

        assertThatThrownBy(() -> service.generate(OrganizationInviteCodeGenerateParamsBO.builder()
                .bizType(OTPType.OrganizationUserInvite)
                .creatorId(1L)
                .expiredAt(LocalDateTime.now(CLOCK).plusHours(1))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("目标组织不能为空");

        when(organizationMapper.selectById(2L)).thenReturn(null);
        assertThatThrownBy(() -> service.generate(OrganizationInviteCodeGenerateParamsBO.builder()
                .bizType(OTPType.OrganizationUserInvite)
                .creatorId(1L)
                .orgId(2L)
                .expiredAt(LocalDateTime.now(CLOCK).plusHours(1))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织不存在或已禁用");

        when(organizationInviteCodeMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.generate(OrganizationInviteCodeGenerateParamsBO.builder()
                .bizType(OTPType.OrganizationInvite)
                .creatorId(1L)
                .expiredAt(LocalDateTime.now(CLOCK).plusHours(1))
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码生成失败");
        verify(organizationInviteCodeMapper, never()).insert(any(OrganizationInviteCode.class));
    }

    @Test
    void shouldRejectAcceptWhenRecordIsMissingOrExpired() {
        when(organizationInviteCodeMapper.update(isNull(), any())).thenReturn(0);
        when(organizationInviteCodeMapper.selectById(8L)).thenReturn(null);
        assertThatThrownBy(() -> service.accept(OrganizationInviteCodeAcceptParamsBO.builder().id(8L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码无效");

        when(organizationInviteCodeMapper.selectById(8L)).thenReturn(OrganizationInviteCode.builder()
                .id(8L)
                .accepted(0)
                .expiredAt(LocalDateTime.now(CLOCK).minusMinutes(1))
                .build());
        assertThatThrownBy(() -> service.accept(OrganizationInviteCodeAcceptParamsBO.builder()
                .id(8L)
                .orgMemberId(9L)
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码已过期");
    }

    @Test
    void shouldBackfillInviteeAndMemberAfterApproval() {
        service.bindInvitee(8L, 4L, 9L);
        service.bindInvitee(8L, null, 9L);

        verify(organizationInviteCodeMapper, org.mockito.Mockito.times(2)).update(isNull(), any());
    }

    @Test
    void shouldRejectNullExpiry() {
        assertThatThrownBy(() -> service.generate(OrganizationInviteCodeGenerateParamsBO.builder()
                .bizType(OTPType.OrganizationInvite)
                .creatorId(1L)
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("到期时间必须晚于当前时间");
    }
}
