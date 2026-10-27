package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.OTPType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneTimePasswordCreateParamsBO {
    // 验证码
    private String code;
    // 过期时间，为空表示不过期
    private LocalDateTime expiredAt;
    // 使用上限，为空表示不限次数
    private Integer maxUses;
    // 业务类型
    private OTPType bizType;
    // 创建者 ID
    private Long creatorId;
}
