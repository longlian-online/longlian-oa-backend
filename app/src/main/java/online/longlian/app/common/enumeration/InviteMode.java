package online.longlian.app.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

public enum InviteMode {

    @Schema(description = "超管邀请新用户注册并创建组织")
    SUPER_ADMIN_CREATE_ORG,

    @Schema(description = "组织管理员生成的组织邀请码")
    ORG_ADMIN_INVITE
}
