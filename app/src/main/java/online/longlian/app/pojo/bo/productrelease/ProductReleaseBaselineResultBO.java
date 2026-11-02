package online.longlian.app.pojo.bo.productrelease;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProductReleaseBaselineResultBO {
    private String version;
    private String frontendVersion;
    private String backendVersion;
    private LocalDateTime publishedAt;
}
