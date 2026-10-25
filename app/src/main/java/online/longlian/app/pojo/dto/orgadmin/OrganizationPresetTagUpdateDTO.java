package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改组织预制标签请求参数")
public class OrganizationPresetTagUpdateDTO {

    @NotBlank(message = "标签名不能为空")
    @Size(max = 50, message = "标签名不能超过 50 个字符")
    @Schema(description = "标签名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 16, message = "颜色不能超过 16 个字符")
    @Schema(description = "颜色，空或不传表示改为默认。blue、green、amber、rose、violet")
    private String color;

    @Schema(description = "优先级，省略时保持原值。仅影响用户端下拉顺序，越大越靠前")
    private Integer priority;
}
