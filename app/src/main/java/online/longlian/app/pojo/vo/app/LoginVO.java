package online.longlian.app.pojo.vo.app;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Schema(description = "登录返回信息")
public class LoginVO {

    @JsonLongIdString
    @Schema(type = "string", description = "用户id")
    private Long userId;

    @JsonLongIdString
    @Schema(type = "string", description = "默认组织ID，不参与后续鉴权")
    private Long defaultOrgId;

    @Schema(description = "用户认证token")
    private String token;

    @Schema(description = "默认组织内的角色，仅供展示。没有可用默认组织时为空")
    private String role;
}
