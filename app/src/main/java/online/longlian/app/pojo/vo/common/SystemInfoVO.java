package online.longlian.app.pojo.vo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "系统信息")
public class SystemInfoVO {

    @Schema(description = "后端版本号", example = "1.0.13-SNAPSHOT")
    private String version;

    @Schema(description = "后端构建时间（UTC ISO-8601），缺失时为空", example = "2026-10-19T02:30:00Z")
    private String buildTime;

    @Schema(description = "系统显示名称，取自 longlian.display-name", example = "龙联 OA")
    private String displayName;
}
