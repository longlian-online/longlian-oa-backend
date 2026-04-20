package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.generator.enumeration.OTPType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
