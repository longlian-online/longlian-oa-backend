package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前用户配置")
public class UserConfigVO {
    @Schema(description = "完整用户配置，未配置时为空对象", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> config;
}
