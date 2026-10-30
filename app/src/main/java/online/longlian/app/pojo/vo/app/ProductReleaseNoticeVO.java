package online.longlian.app.pojo.vo.app;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductReleaseNoticeVO {
    private Long id;
    private String version;
    private String title;
    private String summary;
    private LocalDateTime publishedAt;
    private List<String> added;
    private List<String> changed;
    private List<String> fixed;
}
