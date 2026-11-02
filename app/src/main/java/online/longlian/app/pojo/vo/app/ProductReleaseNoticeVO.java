package online.longlian.app.pojo.vo.app;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductReleaseNoticeVO {
    private Long id;
    private String version;
    private String title;
    private String summary;
    private String content;
    private LocalDateTime publishedAt;
}
