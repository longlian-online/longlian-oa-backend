package online.longlian.app.service.otp;

import online.longlian.app.pojo.bo.common.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.common.enumeration.OTPType;

/**
 * 一次性密码（OTP）持久化服务接口。
 * <p>
 * 负责 OTP 的生成、校验和使用等底层持久化操作，不包含具体业务逻辑。
 * 各类 OTP 的业务流程（如邮箱验证码发送、组织邀请码消费后的后续处理）
 * 由 {@link OTPStrategyService} 各实现封装。
 */
public interface OneTimePasswordService {

    /**
     * 生成一次性密码并持久化。
     *
     * @param params 包含业务类型、过期时间及关联目标（如邮箱/用户 ID）的生成参数
     * @return 已持久化的一次性密码实体
     */
    OneTimePassword generateOTP(OneTimePasswordCreateParamsBO params);

    /**
     * 获取有效且未使用的一次性密码。
     *
     * @param code    验证码
     * @param bizType 业务类型（区分邮箱验证码/组织邀请码等）
     * @return 有效的一次性密码，不存在或已过期/已使用时抛出异常
     */
    OneTimePassword getValidOTP(String code, OTPType bizType);

    /**
     * 使用（消费）一次性密码，将其标记为已使用。
     *
     * @param otpId 一次性密码 ID
     */
    void useOTP(Long otpId);
}
