package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "刷新会话返回。只换发凭证，不改变当前组织。")
public class SessionRefreshVO {

    @Schema(description = "新的访问凭证")
    private String token;

    @Schema(description = "新的刷新凭证")
    private String refreshToken;
}
