package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "切换组织，id参数")
public class OrgIdDTO {

    @NotBlank(message = "组织id不能为空")
    @Schema(description = "组织id")
    private Long orgId;
}