package online.longlian.app.pojo.bo;

import online.longlian.app.common.enumeration.OTPType;

import java.time.LocalDateTime;

public class OneTimePasswordCreateParamsBO {
    // 验证码
    private String code;
    // 过期时间
    private LocalDateTime expiredAt;
    // 业务类型
    private OTPType bizType;
    // 创建者 ID
    private Long creatorId;
}
