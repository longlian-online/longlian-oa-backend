package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "已发布的产品版本公告")
public class ProductReleaseNoticeVO {

    @JsonLongIdString
    @Schema(type = "string", description = "产品版本ID")
    private Long id;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "更新说明，Markdown")
    private String content;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
}
