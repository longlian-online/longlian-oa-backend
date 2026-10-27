package online.longlian.app.service.otp.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OneTimePasswordMapper;
import online.longlian.app.pojo.bo.common.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.service.otp.OneTimePasswordService;
import online.longlian.common.enumeration.OTPStatus;
import online.longlian.common.enumeration.OTPType;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OneTimePasswordServiceImpl extends ServiceImpl<OneTimePasswordMapper, OneTimePassword> implements OneTimePasswordService {

    private final OneTimePasswordMapper oneTimePasswordMapper;
    private final Clock clock;

    @Override
    public OneTimePassword generateOTP(OneTimePasswordCreateParamsBO params) {
        boolean unlimited = params.getMaxUses() == null;
        if (!unlimited && params.getMaxUses() < 1) {
            throw new AppException(ResultCode.PARAM_ERROR, "使用次数必须为正整数");
        }
        // 不限次数只属于没有到期时间的加入组织邀请码，其余类型仍必须一次到期。
        if (unlimited) {
            if (params.getBizType() != OTPType.OrganizationUserInvite || params.getExpiredAt() != null) {
                throw new AppException(ResultCode.PARAM_ERROR, "只有加入组织邀请码可以不限次数");
            }
        } else if (params.getExpiredAt() == null && params.getBizType() != OTPType.OrganizationUserInvite) {
            throw new AppException(ResultCode.PARAM_ERROR, "到期时间必须晚于当前时间");
        }
        OneTimePassword oneTimePassword = OneTimePassword.builder()
                .code(params.getCode())
                .expiredAt(requireFutureExpiry(params.getExpiredAt()))
                .maxUses(params.getMaxUses())
                .useCount(0)
                .bizType(params.getBizType())
                .status(OTPStatus.PENDING)
                .creatorId(params.getCreatorId())
                .build();
        oneTimePasswordMapper.insert(oneTimePassword);
        return oneTimePassword;
    }

    private LocalDateTime requireFutureExpiry(LocalDateTime expiredAt) {
        if (expiredAt != null && !expiredAt.isAfter(LocalDateTime.now(clock))) {
            throw new AppException(ResultCode.PARAM_ERROR, "到期时间必须晚于当前时间");
        }
        return expiredAt;
    }

    @Override
    public OneTimePassword getValidOTP(String code, OTPType bizType) {
        OneTimePassword oneTimePassword = oneTimePasswordMapper.selectOne(
                new LambdaQueryWrapper<OneTimePassword>()
                        .eq(OneTimePassword::getCode, code)
                        .eq(OneTimePassword::getBizType, bizType)
                        .eq(OneTimePassword::getStatus, OTPStatus.PENDING)
                        .orderByDesc(OneTimePassword::getCreatedAt)
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

        boolean finalUse = isFinalUse(oneTimePassword);
        LambdaUpdateWrapper<OneTimePassword> update = new LambdaUpdateWrapper<OneTimePassword>()
                .eq(OneTimePassword::getId, otpId)
                .eq(OneTimePassword::getStatus, OTPStatus.PENDING)
                .setSql("use_count = use_count + 1");
        if (oneTimePassword.getExpiredAt() == null) {
            update.isNull(OneTimePassword::getExpiredAt);
        } else {
            update.gt(OneTimePassword::getExpiredAt, LocalDateTime.now(clock));
        }
        if (oneTimePassword.getMaxUses() == null) {
            update.isNull(OneTimePassword::getMaxUses);
        } else {
            update.apply("use_count < {0}", oneTimePassword.getMaxUses());
        }
        // 一次即失效的旧记录仍以使用时间为准；可重复使用的记录只看剩余次数。
        if (oneTimePassword.getMaxUses() != null && oneTimePassword.getMaxUses() == 1) {
            update.isNull(OneTimePassword::getUsedAt);
        }
        if (finalUse) {
            update.set(OneTimePassword::getUsedAt, LocalDateTime.now(clock))
                    .set(OneTimePassword::getStatus, OTPStatus.USED);
        }
        int rows = oneTimePasswordMapper.update(null, update);
        if (rows == 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, oneTimePassword.getBizType().getDesc() + "已被使用");
        }
    }

    private boolean isFinalUse(OneTimePassword oneTimePassword) {
        int useCount = oneTimePassword.getUseCount() == null ? 0 : oneTimePassword.getUseCount();
        return oneTimePassword.getMaxUses() != null && useCount + 1 >= oneTimePassword.getMaxUses();
    }

    private void validateOtpAvailability(OneTimePassword oneTimePassword, OTPType bizType) {
        if (oneTimePassword == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, bizType.getDesc() + "不存在");
        }
        if (oneTimePassword.getStatus() == OTPStatus.REVOKED) {
            throw new AppException(ResultCode.OPERATION_FAIL, bizType.getDesc() + "已失效");
        }
        if (oneTimePassword.getStatus() == OTPStatus.USED || isExhausted(oneTimePassword)) {
            throw new AppException(ResultCode.OPERATION_FAIL, bizType.getDesc() + "已使用");
        }
        if (oneTimePassword.getExpiredAt() != null && !oneTimePassword.getExpiredAt().isAfter(LocalDateTime.now(clock))) {
            throw new AppException(ResultCode.OPERATION_FAIL, bizType.getDesc() + "已过期");
        }
    }

    private boolean isExhausted(OneTimePassword oneTimePassword) {
        if (oneTimePassword.getMaxUses() == null) {
            return false;
        }
        int useCount = oneTimePassword.getUseCount() == null ? 0 : oneTimePassword.getUseCount();
        return useCount >= oneTimePassword.getMaxUses();
    }
}
