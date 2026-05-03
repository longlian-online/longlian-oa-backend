package online.longlian.app.service;

import online.longlian.app.pojo.entity.OneTimePassword;

public interface VerifyCodeService {

    /**
     * 发送邮箱验证码
     */
    boolean sendCode(String receiver);

    /**
     * 验证邮箱验证码
     *
     * @return 可用的邮箱验证码 OTP
     */
    OneTimePassword validateCode(String receiver, String code);
}
