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
@Schema(description = "邀请码（可用于创建组织、注册入组或已注册用户申请入组）")
public class InviteCodeVO {

    @Schema(description = "邀请码（6位字母数字，一次性使用，有效期30分钟）")
    private String inviteCode;

    @Schema(description = "邀请码过期时间")
    private String expireAt;
}
