package online.longlian.app.service.otp;

import online.longlian.app.pojo.bo.common.OTPGenerateContextBO;
import online.longlian.app.pojo.bo.common.OTPUseContextBO;
import online.longlian.app.pojo.bo.common.OTPValidateContextBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.common.enumeration.OTPType;

/**
 * OTP 生命周期策略接口。
 * <p>
 * 每种 OTP 类型（邮箱验证码、组织邀请码、用户邀请码等）
 * 封装完整的生成、校验、使用逻辑，由 {@link OTPServiceFactory} 自动注册和路由。
 * <p>
 * 典型实现示例：{@link online.longlian.app.service.otp.impl.EmailVerifyService}（生成后异步发送邮件，
 * 事务提交后才发送以避免回滚导致验证码无效）。
 */
public interface OTPStrategyService {

    /**
     * 自标识所属 OTP 类型，供 {@link OTPServiceFactory} 自动注册和路由。
     *
     * @return 当前策略对应的 OTP 类型枚举
     */
    OTPType getOtpType();

    /**
     * 生成一次性验证码并执行类型特定的后置动作。
     * <p>
     * 如邮箱验证码生成后异步发送邮件，组织邀请码生成后关联组织 ID。
     *
     * @param otpGenerateContextBO 生成上下文，包含业务类型、目标、创建者等信息
     * @return 已生成并持久化的一次性密码
     */
    OneTimePassword generate(OTPGenerateContextBO otpGenerateContextBO);

    /**
     * 校验验证码有效性（未过期、未使用、类型匹配）。
     *
     * @param otpValidateContextBO 校验上下文，包含验证码和可选的校验目标
     * @return 校验通过的一次性密码实体
     */
    OneTimePassword getValid(OTPValidateContextBO otpValidateContextBO);

    /**
     * 消费一次性密码，将其标记为已使用并执行后置处理。
     * <p>
     * 如组织邀请码消费后写入关联记录，验证码消费后记录使用时间。
     *
     * @param otpUseContextBO 使用上下文，包含 OTP ID 和可选的业务关联信息
     */
    void use(OTPUseContextBO otpUseContextBO);
}
