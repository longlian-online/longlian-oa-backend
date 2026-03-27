package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "邀请码（管理员生成，供已登录用户加入组织使用）")
public class InviteCodeVO {

    @Schema(description = "邀请码（6位字母数字，一次性使用，有效期30分钟）")
    private String inviteCode;

    @Schema(description = "邀请码过期时间")
    private String expireAt;
}
