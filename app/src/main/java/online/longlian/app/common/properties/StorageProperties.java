package online.longlian.app.common.properties;

import lombok.Data;
import online.longlian.common.enumeration.StorageType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {
    private StorageType type;
    private long presignedUrlTtlSeconds = 300;
    private CdnConfig cdn;
    private LocalConfig local;
    private OssConfig oss;
    private CosConfig cos;

    @Data
    public static class CdnConfig {
        private boolean enabled;
        private String urlPrefix;
        private String authKey;
    }

    @Data
    public static class LocalConfig {
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
        private String bucket;
        private String secretId;
        private String region;
        private String secretKey;
    }
}
