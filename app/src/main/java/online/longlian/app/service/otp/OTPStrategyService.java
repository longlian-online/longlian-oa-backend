package online.longlian.app.service.otp;

import online.longlian.app.pojo.bo.OTPGenerateContextBO;
import online.longlian.app.pojo.bo.OTPUseContextBO;
import online.longlian.app.pojo.bo.OTPValidateContextBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.common.enumeration.OTPType;

/**
 * OTP 生命周期策略接口 —— 每种 OTP 类型（邮箱验证码/组织邀请码等）封装完整的生成、校验、使用逻辑。
 */
public interface OTPStrategyService {

    /**
     * 自标识所属 OTP 类型，供 {@link OTPServiceFactory} 自动注册和路由。
     */
    OTPType getOtpType();

    /**
     * 生成一次性验证码并执行类型特定的后置动作。
     */
    OneTimePassword generate(OTPGenerateContextBO otpGenerateContextBO);

    /**
     * 校验验证码有效性。
     */
    OneTimePassword getValid(OTPValidateContextBO otpValidateContextBO);

    /**
     * 使用一次性密码
     */
    void use(OTPUseContextBO otpUseContextBO);
}
