package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "邀请链接（管理员生成，供新用户注册使用）")
public class InviteLinkVO {

    @Schema(description = "邀请链接（一次性使用，有效期30分钟，用于新用户注册并自动提交入组申请）")
    private String inviteUrl;

    @Schema(description = "链接过期时间")
    private String expireAt;
}
