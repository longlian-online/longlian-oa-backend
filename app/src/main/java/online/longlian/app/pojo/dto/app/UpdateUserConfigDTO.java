package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "更新当前用户配置")
public class UpdateUserConfigDTO {
    @NotNull(message = "用户配置不能为空")
    @Schema(description = "完整用户配置对象", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> config;
}
