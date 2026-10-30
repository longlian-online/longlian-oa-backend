package online.longlian.app.pojo.vo.admin;

import lombok.Builder;
import lombok.Data;
import online.longlian.common.enumeration.ProductReleaseStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductReleaseDetailVO {
    private Long id;
    private String version;
    private String frontendVersion;
    private String backendVersion;
    private String title;
    private String summary;
    private ProductReleaseStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
    private List<String> added;
    private List<String> changed;
    private List<String> fixed;
}
