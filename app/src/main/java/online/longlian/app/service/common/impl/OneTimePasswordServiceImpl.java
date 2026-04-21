package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OneTimePasswordMapper;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.service.common.OneTimePasswordService;
import online.longlian.generator.enumeration.OTPType;
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
        validateOtpAvailability(oneTimePassword);
        return oneTimePassword;
    }

    @Override
    public void useOTP(Long otpId) {
        OneTimePassword oneTimePassword = oneTimePasswordMapper.selectById(otpId);
        validateOtpAvailability(oneTimePassword);

        oneTimePasswordMapper.update(
                null,
                new LambdaUpdateWrapper<OneTimePassword>()
                        .eq(OneTimePassword::getId, otpId)
                        .isNull(OneTimePassword::getUsedAt)
                        .gt(OneTimePassword::getExpiredAt, LocalDateTime.now())
                        .set(OneTimePassword::getUsedAt, LocalDateTime.now())
        );
    }

    private void validateOtpAvailability(OneTimePassword oneTimePassword) {
        if (oneTimePassword == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码不存在");
        }
        if (oneTimePassword.getUsedAt() != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码已使用");
        }
        if (oneTimePassword.getExpiredAt() == null || !oneTimePassword.getExpiredAt().isAfter(LocalDateTime.now())) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码已过期");
        }
    }
}