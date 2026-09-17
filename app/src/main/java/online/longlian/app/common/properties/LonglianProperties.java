package online.longlian.app.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "longlian")
@Data
public class LonglianProperties {
    private String serverUrl;
}
