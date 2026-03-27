package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "邀请信息（邀请码 + 邀请链接）")
public class InviteLinkVO {

    @Schema(description = "邀请码（6位字母数字）")
    private String inviteCode;

    @Schema(description = "邀请链接（一次性使用，有效期30分钟）")
    private String inviteUrl;

}
