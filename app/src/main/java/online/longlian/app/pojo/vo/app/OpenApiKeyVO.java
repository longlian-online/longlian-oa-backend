package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "OpenAPI 密钥信息（不含明文）")
public class OpenApiKeyVO {

    @Schema(description = "密钥ID")
    private Long id;

    @Schema(description = "密钥名称")
    private String name;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "最近一次成功认证时间，未使用过为 null")
    private LocalDateTime lastUsedAt;
}
