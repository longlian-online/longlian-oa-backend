package online.longlian.app.pojo.bo.productrelease;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductReleaseNoticeResultBO {
    private Long id;
    private String version;
    private String title;
    private String summary;
    private LocalDateTime publishedAt;
    private List<String> added;
    private List<String> changed;
    private List<String> fixed;
}
