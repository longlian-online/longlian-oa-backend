package online.longlian.app.service.common;

import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.OneTimePassword;

public interface OneTimePasswordService {

    /**
     * 生成一次性密码
     * @param params 参数
     * @return 已保存的一次性密码
     */
    OneTimePassword generateOTP(OneTimePasswordCreateParamsBO params);

    /**
     * 使用一次性密码
     * @param code 一次性密码
     */
    void useOTP(String code);
}
