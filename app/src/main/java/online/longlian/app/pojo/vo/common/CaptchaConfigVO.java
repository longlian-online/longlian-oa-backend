package online.longlian.app.pojo.vo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "人机验证公开配置")
public class CaptchaConfigVO {
    @Schema(description = "是否启用人机验证")
    private boolean enabled;

    @Schema(description = "浏览器访问的人机验证服务地址，仅启用时返回")
    private String serverUrl;

    @Schema(description = "公开站点标识，仅启用时返回")
    private String siteKey;
}
