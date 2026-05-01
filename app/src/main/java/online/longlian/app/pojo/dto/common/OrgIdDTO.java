package online.longlian.app.pojo.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "切换组织，id参数")
public class OrgIdDTO {

    @NotNull(message = "组织id不能为空")
    @Schema(type = "string", description = "组织id")
    private Long orgId;
}
