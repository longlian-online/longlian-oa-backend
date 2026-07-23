package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.EmailVerifyBusinessType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTPValidateContextBO {
    private String code;
    private String target;
    private EmailVerifyBusinessType businessType;
}
