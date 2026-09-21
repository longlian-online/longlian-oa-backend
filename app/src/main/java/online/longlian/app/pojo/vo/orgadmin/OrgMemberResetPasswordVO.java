package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成员密码重置结果")
public class OrgMemberResetPasswordVO {

    @Schema(description = "仅本次响应可见的随机密码")
    private String password;
}
