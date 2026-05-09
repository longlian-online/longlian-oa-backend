package online.longlian.app.service.otp;

import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.common.enumeration.OTPType;

public interface OneTimePasswordService {

    /**
     * 生成一次性密码
     * @param params 参数
     * @return 已保存的一次性密码
     */
    OneTimePassword generateOTP(OneTimePasswordCreateParamsBO params);

    /**
     * 获取可用的一次性密码
     * @param code 验证码
     * @param bizType 业务类型
     * @return 可用的一次性密码
     */
    OneTimePassword getValidOTP(String code, OTPType bizType);

    /**
     * 使用一次性密码
     * @param otpId 一次性密码ID
     */
    void useOTP(Long otpId);
}
