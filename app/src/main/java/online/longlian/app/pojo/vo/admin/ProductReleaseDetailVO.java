package online.longlian.app.pojo.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.ProductReleaseStatus;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "产品版本详情")
public class ProductReleaseDetailVO {

    @JsonLongIdString
    @Schema(type = "string", description = "产品版本ID")
    private Long id;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "前端版本号")
    private String frontendVersion;

    @Schema(description = "后端版本号")
    private String backendVersion;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "更新说明，Markdown")
    private String content;

    @Schema(description = "发布状态")
    private ProductReleaseStatus status;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
