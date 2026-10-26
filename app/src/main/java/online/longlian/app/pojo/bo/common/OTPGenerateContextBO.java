package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.EmailVerifyBusinessType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTPGenerateContextBO {
    private Long creatorId;
    private String receiver;
    private EmailVerifyBusinessType businessType;
    private Long orgId;
    private LocalDateTime expiredAt;
}
