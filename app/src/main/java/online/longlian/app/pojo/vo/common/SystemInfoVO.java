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
}
