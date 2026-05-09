package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.generator.enumeration.EmailVerifyBusinessType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTPGenerateContextBO {
    private Long creatorId;
    private String receiver;
    private EmailVerifyBusinessType businessType;
    private Long orgId;
}
