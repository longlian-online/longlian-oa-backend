package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建 OpenAPI 密钥请求")
public class OpenApiKeyCreateDTO {

    @Schema(description = "密钥名称")
    @NotBlank
    @Size(max = 50)
    private String name;
}
