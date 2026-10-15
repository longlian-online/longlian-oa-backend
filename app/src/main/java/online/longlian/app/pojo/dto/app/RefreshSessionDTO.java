package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "刷新会话请求")
public class RefreshSessionDTO {

    @NotBlank(message = "刷新凭证不能为空")
    @Schema(description = "登录时签发的刷新凭证", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
