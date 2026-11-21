package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "按组织分组的 OpenAPI 密钥")
public class OpenApiKeyGroupVO {

    @Schema(description = "组织ID")
    private Long orgId;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "该组织允许持有的最大密钥数")
    private Integer maxKeys;

    @Schema(description = "该组织的启用密钥")
    private List<OpenApiKeyVO> keys;
}
