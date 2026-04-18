package online.longlian.app.service.common;

import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;

public interface OneTimePasswordService {

    /**
     * 生成一次性密码
     * @param params 参数
     */
    void generateOTP(OneTimePasswordCreateParamsBO params);

    /**
     * 使用一次性密码
     * @param code 一次性密码
     */
    void useOTP(String code);
}
