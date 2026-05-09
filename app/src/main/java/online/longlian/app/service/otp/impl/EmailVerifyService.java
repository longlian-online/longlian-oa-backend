package online.longlian.app.service.otp.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.constants.PatternConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.RandomCodeUtil;
import online.longlian.app.mapper.EmailVerifyOtpMapper;
import online.longlian.app.pojo.bo.OTPGenerateContextBO;
import online.longlian.app.pojo.bo.OTPUseContextBO;
import online.longlian.app.pojo.bo.OTPValidateContextBO;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.EmailVerifyOtp;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.service.otp.OTPStrategyService;
import online.longlian.app.service.otp.OneTimePasswordService;
import online.longlian.common.enumeration.EmailVerifyBusinessType;
import online.longlian.common.enumeration.EmailVerifySendStatus;
import online.longlian.common.enumeration.OTPType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerifyService implements OTPStrategyService {

    private final OneTimePasswordService oneTimePasswordService;
    private final EmailVerifyOtpMapper emailVerifyOtpMapper;
    private final EmailVerifyCodeAsyncSender emailVerifyCodeAsyncSender;

    @Override
    public OTPType getOtpType() {
        return OTPType.EmailVerify;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OneTimePassword generate(OTPGenerateContextBO otpGenerateContextBO) {
        String receiver = otpGenerateContextBO.getReceiver();
        EmailVerifyBusinessType businessType = otpGenerateContextBO.getBusinessType();
        if (!isValidEmail(receiver)) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邮箱格式不合法");
        }

        // 限制60s内发送验证码
        LocalDateTime now = LocalDateTime.now();
        Long recentCount = emailVerifyOtpMapper.selectCount(
                new LambdaQueryWrapper<EmailVerifyOtp>()
                        .eq(EmailVerifyOtp::getReceiver, receiver)
                        .in(EmailVerifyOtp::getSendStatus, List.of(EmailVerifySendStatus.PENDING, EmailVerifySendStatus.SENT))
                        .gt(EmailVerifyOtp::getCreatedAt, now.minusSeconds(CommonConstants.CODE_LIMIT))
        );
        if (recentCount > 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "发送验证码过于频繁");
        }

        String code = RandomCodeUtil.generateCode(CommonConstants.CODE_LENGTH);
        OneTimePassword oneTimePassword = oneTimePasswordService.generateOTP(
                OneTimePasswordCreateParamsBO.builder()
                        .code(code)
                        .expiredAt(now.plusSeconds(CommonConstants.CODE_EXPIRE))
                        .bizType(OTPType.EmailVerify)
                        .creatorId(otpGenerateContextBO.getCreatorId() != null ? otpGenerateContextBO.getCreatorId() : 0L)
                        .build()
        );

        EmailVerifyOtp emailVerifyOtp = EmailVerifyOtp.builder()
                .otpId(oneTimePassword.getId())
                .receiver(receiver)
                .businessType(businessType)
                .sendStatus(EmailVerifySendStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        emailVerifyOtpMapper.insert(emailVerifyOtp);

        // 事务提交后才异步发邮件：避免邮件发出后事务回滚导致验证码无效
        sendAfterCommit(emailVerifyOtp.getId(), receiver, code);

        return oneTimePassword;
    }

    @Override
    public OneTimePassword getValid(OTPValidateContextBO otpValidateContextBO) {
        String code = otpValidateContextBO.getCode();
        OneTimePassword oneTimePassword = oneTimePasswordService.getValidOTP(code, OTPType.EmailVerify);
        // 校验发送状态和收件人：只允许已成功发送且收件人匹配的验证码通过
        EmailVerifyOtp emailVerifyOtp = emailVerifyOtpMapper.selectOne(
                new LambdaQueryWrapper<EmailVerifyOtp>()
                        .eq(EmailVerifyOtp::getOtpId, oneTimePassword.getId())
                        .eq(EmailVerifyOtp::getReceiver, otpValidateContextBO.getTarget())
                        .eq(EmailVerifyOtp::getSendStatus, EmailVerifySendStatus.SENT)
                        .last("LIMIT 1")
        );
        if (emailVerifyOtp == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "验证码不存在");
        }
        return oneTimePassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void use(OTPUseContextBO otpUseContextBO) {
        oneTimePasswordService.useOTP(otpUseContextBO.getOtpId());
    }

    /**
     * 如果当前在事务中则注册回调等待提交后发送，否则直接发送。
     * 保证 OTP 记录已持久化再发邮件。
     */
    private void sendAfterCommit(Long emailVerifyOtpId, String receiver, String code) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailVerifyCodeAsyncSender.send(emailVerifyOtpId, receiver, code);
                }
            });
            return;
        }
        emailVerifyCodeAsyncSender.send(emailVerifyOtpId, receiver, code);
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return Pattern.compile(PatternConstants.EMAIL_PATTERN).matcher(email.trim()).matches();
    }
}
