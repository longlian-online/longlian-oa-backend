package online.longlian.app.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 开放接口配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "longlian.open-api")
public class OpenApiProperties {

    /** 每用户每组织最多持有的启用密钥数 */
    private int maxKeysPerOrg = 2;

    /** 每密钥每分钟请求上限，超限返回 429 */
    private int rateLimitPerMinute = 60;
}
