package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "生成加入组织邀请码请求参数")
public class GenerateJoinInviteCodeDTO {

    @NotNull(message = "到期时间不能为空")
    @Future(message = "到期时间必须晚于当前时间")
    @Schema(description = "邀请人选择的到期时间", example = "2026-10-27 18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expiredAt;

    @Min(value = 1, message = "使用次数必须为正整数")
    @Schema(description = "使用上限；不传表示不限次数", example = "5")
    private Integer maxUses;
}
