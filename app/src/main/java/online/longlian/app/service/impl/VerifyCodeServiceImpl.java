package online.longlian.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.constants.PatternConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.RandomCodeUtil;
import online.longlian.app.mapper.EmailVerifyOtpMapper;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.EmailVerifyOtp;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.service.VerifyCodeService;
import online.longlian.app.service.common.OneTimePasswordService;
import online.longlian.generator.enumeration.EmailVerifySendStatus;
import online.longlian.generator.enumeration.OTPType;
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
public class VerifyCodeServiceImpl implements VerifyCodeService {

    private final OneTimePasswordService oneTimePasswordService;
    private final EmailVerifyOtpMapper emailVerifyOtpMapper;
    private final EmailVerifyCodeAsyncSender emailVerifyCodeAsyncSender;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendCode(String receiver) {
        if (!isValidEmail(receiver)) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邮箱格式不合法");
        }

        // 检查最近60秒内是否有发送记录
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
        OneTimePassword otp = oneTimePasswordService.generateOTP(
                OneTimePasswordCreateParamsBO.builder()
                        .code(code)
                        .expiredAt(now.plusSeconds(CommonConstants.CODE_EXPIRE))
                        .bizType(OTPType.EmailVerify)
                        .creatorId(0L)
                        .build()
        );

        EmailVerifyOtp emailVerifyOtp = EmailVerifyOtp.builder()
                .otpId(otp.getId())
                .receiver(receiver)
                .sendStatus(EmailVerifySendStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        emailVerifyOtpMapper.insert(emailVerifyOtp);

        sendAfterCommit(emailVerifyOtp.getId(), receiver, code);

        return true;
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OneTimePassword validateCode(String receiver, String code) {
        OneTimePassword otp = oneTimePasswordService.getValidOTP(code, OTPType.EmailVerify);
        EmailVerifyOtp emailVerifyOtp = emailVerifyOtpMapper.selectOne(
                new LambdaQueryWrapper<EmailVerifyOtp>()
                        .eq(EmailVerifyOtp::getOtpId, otp.getId())
                        .eq(EmailVerifyOtp::getReceiver, receiver)
                        .eq(EmailVerifyOtp::getSendStatus, EmailVerifySendStatus.SENT)
        );
        if (emailVerifyOtp == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "验证码不存在");
        }
        return otp;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return Pattern.compile(PatternConstants.EMAIL_PATTERN).matcher(email.trim()).matches();
    }
}
