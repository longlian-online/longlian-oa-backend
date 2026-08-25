package online.longlian.app.common.properties;

import lombok.Data;
import online.longlian.common.enumeration.StorageType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {
    private StorageType type;
    private LocalConfig local;
    private OssConfig oss;
    private CosConfig cos;

    @Data
    public static class LocalConfig {
        private String baseUrl;
        private String directory;
    }

    @Data
    public static class OssConfig {
        private String baseUrl;
        private String bucket;
        private String secretId;
        private String region;
        private String secretKey;
    }

    @Data
    public static class CosConfig {
        private String urlPrefix;
        private String bucket;
        private String secretId;
        private String region;
        private String secretKey;
    }
}
