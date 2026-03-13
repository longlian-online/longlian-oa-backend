package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "登录返回信息")
public class LoginVO {

    @Schema(description = "用户token")
    private String token;

    @Schema(description = "用户角色")
    private List<String> roles;
}
