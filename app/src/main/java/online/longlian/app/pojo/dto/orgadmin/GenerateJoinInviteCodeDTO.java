package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "生成加入组织邀请码请求参数")
public class GenerateJoinInviteCodeDTO {

    @Min(value = 1, message = "使用次数必须为正整数")
    @Schema(description = "使用上限；不传表示不限次数", example = "5")
    private Integer maxUses;
}
