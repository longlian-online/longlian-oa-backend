package online.longlian.app.pojo.dto.common;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "切换组织，id参数")
public class OrgIdDTO {

    @NotNull(message = "组织id不能为空")
    @JsonLongIdString
    @Schema(type = "string", description = "组织id")
    private Long orgId;
}
