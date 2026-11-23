package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建 OpenAPI 密钥的结果，key 明文只在创建响应出现一次")
public class OpenApiKeyCreatedVO {

    @Schema(description = "密钥ID")
    private Long id;

    @Schema(description = "完整密钥明文，仅此一次返回")
    private String key;
}
