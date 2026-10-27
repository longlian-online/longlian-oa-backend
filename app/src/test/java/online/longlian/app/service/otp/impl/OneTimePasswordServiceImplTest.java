package online.longlian.app.service.otp.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.OneTimePasswordMapper;
import online.longlian.app.pojo.bo.common.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.common.enumeration.OTPStatus;
import online.longlian.common.enumeration.OTPType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneTimePasswordServiceImplTest {

    @Mock
    private OneTimePasswordMapper oneTimePasswordMapper;

    private OneTimePasswordServiceImpl service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OneTimePassword.class);
        service = new OneTimePasswordServiceImpl(oneTimePasswordMapper, clock);
    }

    @Test
    void generateOTP_insertsAndReturns() {
        OneTimePasswordCreateParamsBO params = OneTimePasswordCreateParamsBO.builder()
                .code("123456")
                .expiredAt(LocalDateTime.now(clock).plusMinutes(5))
                .bizType(OTPType.EmailVerify)
                .creatorId(1L)
                .build();
        when(oneTimePasswordMapper.insert(any(OneTimePassword.class))).thenReturn(1);

        OneTimePassword result = service.generateOTP(params);

        assertThat(result.getCode()).isEqualTo("123456");
        assertThat(result.getStatus()).isEqualTo(OTPStatus.PENDING);
        assertThat(result.getUseCount()).isZero();
        verify(oneTimePasswordMapper).insert(any(OneTimePassword.class));
    }

    @Test
    void generateOTP_allowsJoinInviteWithoutExpiry() {
        OneTimePasswordCreateParamsBO params = OneTimePasswordCreateParamsBO.builder()
                .code("JOIN01")
                .maxUses(null)
                .bizType(OTPType.OrganizationUserInvite)
                .creatorId(1L)
                .build();
        when(oneTimePasswordMapper.insert(any(OneTimePassword.class))).thenReturn(1);

        OneTimePassword result = service.generateOTP(params);

        assertThat(result.getExpiredAt()).isNull();
        assertThat(result.getMaxUses()).isNull();
    }

    @Test
    void generateOTP_rejectsExpiryThatIsNotInTheFuture() {
        OneTimePasswordCreateParamsBO missing = OneTimePasswordCreateParamsBO.builder()
                .code("123456")
                .bizType(OTPType.OrganizationInvite)
                .creatorId(1L)
                .build();
        OneTimePasswordCreateParamsBO past = OneTimePasswordCreateParamsBO.builder()
                .code("123456")
                .expiredAt(LocalDateTime.now(clock))
                .maxUses(1)
                .bizType(OTPType.OrganizationUserInvite)
                .creatorId(1L)
                .build();

        assertThatThrownBy(() -> service.generateOTP(missing))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("到期时间必须晚于当前时间");
        assertThatThrownBy(() -> service.generateOTP(past))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("到期时间必须晚于当前时间");
        verify(oneTimePasswordMapper, never()).insert(any(OneTimePassword.class));
    }

    @Test
    void getValidOTP_notFound_throws() {
        when(oneTimePasswordMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.getValidOTP("000000", OTPType.EmailVerify))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void getValidOTP_alreadyUsed_throws() {
        OneTimePassword otp = OneTimePassword.builder()
                .id(1L).code("123456").bizType(OTPType.EmailVerify)
                .status(OTPStatus.USED).usedAt(LocalDateTime.now(clock))
                .expiredAt(LocalDateTime.now(clock).plusMinutes(5))
                .build();
        when(oneTimePasswordMapper.selectOne(any())).thenReturn(otp);

        assertThatThrownBy(() -> service.getValidOTP("123456", OTPType.EmailVerify))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("已使用");
    }

    @Test
    void getValidOTP_expired_throws() {
        OneTimePassword otp = OneTimePassword.builder()
                .id(1L).code("123456").bizType(OTPType.EmailVerify)
                .status(OTPStatus.PENDING).usedAt(null)
                .expiredAt(LocalDateTime.now(clock).minusMinutes(1))
                .build();
        when(oneTimePasswordMapper.selectOne(any())).thenReturn(otp);

        assertThatThrownBy(() -> service.getValidOTP("123456", OTPType.EmailVerify))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    void getValidOTP_valid_returnsOtp() {
        OneTimePassword otp = OneTimePassword.builder()
                .id(1L).code("123456").bizType(OTPType.EmailVerify)
                .status(OTPStatus.PENDING).usedAt(null)
                .expiredAt(LocalDateTime.now(clock).plusMinutes(5))
                .build();
        when(oneTimePasswordMapper.selectOne(any())).thenReturn(otp);

        OneTimePassword result = service.getValidOTP("123456", OTPType.EmailVerify);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void useOTP_notFound_throws() {
        when(oneTimePasswordMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.useOTP(99L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("一次性密码不存在");
    }

    @Test
    void useOTP_valid_updatesStatus() {
        OneTimePassword otp = OneTimePassword.builder()
                .id(1L).code("123456").bizType(OTPType.EmailVerify)
                .status(OTPStatus.PENDING).usedAt(null).useCount(0).maxUses(1)
                .expiredAt(LocalDateTime.now(clock).plusMinutes(5))
                .build();
        when(oneTimePasswordMapper.selectById(1L)).thenReturn(otp);
        when(oneTimePasswordMapper.update(isNull(), any())).thenReturn(1);

        service.useOTP(1L);

        verify(oneTimePasswordMapper).update(isNull(), any());
    }

    @Test
    void useOTP_concurrentUse_throws() {
        OneTimePassword otp = OneTimePassword.builder()
                .id(1L).code("123456").bizType(OTPType.EmailVerify)
                .status(OTPStatus.PENDING).usedAt(null).useCount(0).maxUses(1)
                .expiredAt(LocalDateTime.now(clock).plusMinutes(5))
                .build();
        when(oneTimePasswordMapper.selectById(1L)).thenReturn(otp);
        when(oneTimePasswordMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.useOTP(1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("已被使用");
    }

    @Test
    void getValidOTP_unlimitedJoinInviteWithoutExpiry_returnsOtp() {
        OneTimePassword otp = OneTimePassword.builder()
                .id(2L).code("JOIN01").bizType(OTPType.OrganizationUserInvite)
                .status(OTPStatus.PENDING).useCount(4)
                .build();
        when(oneTimePasswordMapper.selectOne(any())).thenReturn(otp);

        assertThat(service.getValidOTP("JOIN01", OTPType.OrganizationUserInvite).getId()).isEqualTo(2L);
    }

    @Test
    void getValidOTP_exhaustedLimitedInvite_throws() {
        OneTimePassword otp = OneTimePassword.builder()
                .id(3L).code("JOIN02").bizType(OTPType.OrganizationUserInvite)
                .status(OTPStatus.PENDING).useCount(2).maxUses(2)
                .build();
        when(oneTimePasswordMapper.selectOne(any())).thenReturn(otp);

        assertThatThrownBy(() -> service.getValidOTP("JOIN02", OTPType.OrganizationUserInvite))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("已使用");
    }

    @Test
    void getValidOTP_revokedInvite_throws() {
        OneTimePassword otp = OneTimePassword.builder()
                .id(4L).code("JOIN03").bizType(OTPType.OrganizationUserInvite)
                .status(OTPStatus.REVOKED).useCount(1)
                .build();
        when(oneTimePasswordMapper.selectOne(any())).thenReturn(otp);

        assertThatThrownBy(() -> service.getValidOTP("JOIN03", OTPType.OrganizationUserInvite))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("已失效");
    }

    @Test
    void useOTP_reusableInvite_keepsPendingUntilLimit() {
        OneTimePassword otp = OneTimePassword.builder()
                .id(5L).code("JOIN04").bizType(OTPType.OrganizationUserInvite)
                .status(OTPStatus.PENDING).useCount(1).maxUses(3)
                .build();
        when(oneTimePasswordMapper.selectById(5L)).thenReturn(otp);
        when(oneTimePasswordMapper.update(isNull(), any())).thenReturn(1);

        service.useOTP(5L);

        verify(oneTimePasswordMapper).update(isNull(), any());
    }
}
