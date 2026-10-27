package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import online.longlian.app.common.annotation.JsonLongIdString;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邀请码（可用于创建组织、注册入组或已注册用户申请入组）")
public class InviteCodeVO {

    @JsonLongIdString
    @Schema(type = "string", description = "邀请记录 ID，仅加入组织邀请码返回")
    private Long id;

    @Schema(description = "邀请码（6位字母数字）")
    private String inviteCode;

    @Schema(description = "使用上限，为空表示不限次数；创建组织邀请码不返回")
    private Integer maxUses;

    @Schema(description = "邀请码过期时间，加入组织邀请码不返回")
    private String expireAt;
}
