package online.longlian.app.pojo.bo.common;

import lombok.Builder;
import lombok.Value;
import online.longlian.app.common.enumeration.CaptchaAction;

@Value
@Builder
public class CaptchaVerifyParamsBO {
    String token;
    CaptchaAction action;
    String remoteIp;
}
