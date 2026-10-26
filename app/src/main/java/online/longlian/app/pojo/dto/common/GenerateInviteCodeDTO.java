package online.longlian.app.pojo.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "生成邀请码请求参数")
public class GenerateInviteCodeDTO {

    @NotNull(message = "到期时间不能为空")
    @Schema(description = "邀请人选择的到期时间", example = "2026-10-27 18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expiredAt;
}
