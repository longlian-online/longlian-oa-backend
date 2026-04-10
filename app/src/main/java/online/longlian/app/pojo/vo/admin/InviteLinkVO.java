package online.longlian.app.pojo.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邀请链接信息")
public class InviteLinkVO {

    @Schema(description = "邀请链接（一次性使用）")
    private String inviteUrl;

    @Schema(description = "邀请token")
    private String inviteToken;

    @Schema(description = "邀请模式：ORG_ADMIN_JOIN(管理员邀请入组)、SUPER_ADMIN_CREATE_ORG(超管邀请建组织)")
    private String inviteMode;

    @Schema(description = "注册页是否需要先调用获取组织信息接口。管理员邀请=true，超管邀请=false")
    private Boolean needFetchOrgInfo;

    @Schema(description = "链接过期时间")
    private String expireAt;
}
