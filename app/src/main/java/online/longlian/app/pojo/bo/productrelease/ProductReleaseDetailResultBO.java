package online.longlian.app.pojo.bo.productrelease;

import lombok.Builder;
import lombok.Data;
import online.longlian.common.enumeration.ProductReleaseStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductReleaseDetailResultBO {
    private Long id;
    private String version;
    private String frontendVersion;
    private String backendVersion;
    private String title;
    private String summary;
    private String content;
    private ProductReleaseStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
