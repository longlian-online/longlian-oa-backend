package online.longlian.app.pojo.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProductReleaseBaselineVO {
    private String version;
    private String frontendVersion;
    private String backendVersion;
    private LocalDateTime publishedAt;
}
