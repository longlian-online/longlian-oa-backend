package online.longlian.app.pojo.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "产品版本基线")
public class ProductReleaseBaselineVO {

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "前端版本号")
    private String frontendVersion;

    @Schema(description = "后端版本号")
    private String backendVersion;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
}
