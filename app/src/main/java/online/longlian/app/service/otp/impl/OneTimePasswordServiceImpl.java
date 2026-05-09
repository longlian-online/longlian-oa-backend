package online.longlian.app.service.otp.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OneTimePasswordMapper;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.service.otp.OneTimePasswordService;
import online.longlian.common.enumeration.OPTStatus;
import online.longlian.common.enumeration.OTPType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OneTimePasswordServiceImpl extends ServiceImpl<OneTimePasswordMapper, OneTimePassword> implements OneTimePasswordService {

    private final OneTimePasswordMapper oneTimePasswordMapper;

    @Override
    public OneTimePassword generateOTP(OneTimePasswordCreateParamsBO params) {
        OneTimePassword oneTimePassword = OneTimePassword.builder()
                .code(params.getCode())
                .expiredAt(params.getExpiredAt())
                .bizType(params.getBizType())
                .status(OPTStatus.PENDING)
                .creatorId(params.getCreatorId())
                .build();
        oneTimePasswordMapper.insert(oneTimePassword);
        return oneTimePassword;
    }

    @Override
    public OneTimePassword getValidOTP(String code, OTPType bizType) {
        OneTimePassword oneTimePassword = oneTimePasswordMapper.selectOne(
                new LambdaQueryWrapper<OneTimePassword>()
                        .eq(OneTimePassword::getCode, code)
                        .eq(OneTimePassword::getBizType, bizType)
                        .last("LIMIT 1")
        );
        validateOtpAvailability(oneTimePassword, bizType);
        return oneTimePassword;
    }

    @Override
    public void useOTP(Long otpId) {
        OneTimePassword oneTimePassword = oneTimePasswordMapper.selectById(otpId);
        if (oneTimePassword == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "一次性密码不存在");
        }
        validateOtpAvailability(oneTimePassword, oneTimePassword.getBizType());

        int rows = oneTimePasswordMapper.update(
                null,
                new LambdaUpdateWrapper<OneTimePassword>()
                        .eq(OneTimePassword::getId, otpId)
                        .eq(OneTimePassword::getStatus, OPTStatus.PENDING)
                        .isNull(OneTimePassword::getUsedAt)
                        .gt(OneTimePassword::getExpiredAt, LocalDateTime.now())
                        .set(OneTimePassword::getUsedAt, LocalDateTime.now())
                        .set(OneTimePassword::getStatus, OPTStatus.USED)
        );
        if (rows == 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, oneTimePassword.getBizType().getDesc() + "已被使用");
        }
    }

    private void validateOtpAvailability(OneTimePassword oneTimePassword, OTPType bizType) {
        if (oneTimePassword == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, bizType.getDesc() + "不存在");
        }
        if (oneTimePassword.getUsedAt() != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, bizType.getDesc() + "已使用");
        }
        if (oneTimePassword.getExpiredAt() == null || !oneTimePassword.getExpiredAt().isAfter(LocalDateTime.now())) {
            throw new AppException(ResultCode.OPERATION_FAIL, bizType.getDesc() + "已过期");
        }
    }
}