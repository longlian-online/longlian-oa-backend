package online.longlian.app.pojo.bo.productrelease;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductReleaseNoticeResultBO {
    private Long id;
    private String version;
    private String title;
    private String summary;
    private String content;
    private LocalDateTime publishedAt;
}
