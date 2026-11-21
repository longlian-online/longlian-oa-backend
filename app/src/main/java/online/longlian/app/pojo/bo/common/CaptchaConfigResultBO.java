package online.longlian.app.pojo.bo.common;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CaptchaConfigResultBO {
    boolean enabled;
    String serverUrl;
    String scriptUrl;
    String siteKey;
}
