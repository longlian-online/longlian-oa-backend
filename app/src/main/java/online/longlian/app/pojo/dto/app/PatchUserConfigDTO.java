package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "部分更新当前用户配置")
public class PatchUserConfigDTO {
    @NotNull(message = "配置变更不能为空")
    @Schema(description = "仅包含本次修改的配置字段；值为 null 时删除该字段", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> config;
}
