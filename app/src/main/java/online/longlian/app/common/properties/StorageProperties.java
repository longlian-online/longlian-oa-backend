package online.longlian.app.common.properties;

import lombok.Builder;
import lombok.Data;
import online.longlian.app.common.enumeration.StorageType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
@Data
@Builder
public class StorageProperties {
    private StorageType type;
    private LocalConfig local;
    private OssConfig oss;

    @Data
    @Builder
    public static class LocalConfig { private String baseUrl; }
    @Data
    @Builder
    public static class OssConfig {
        private String baseUrl;
        private String bucket;
        private String secretId;
        private String region;
        private String secretKey;
    }
}
