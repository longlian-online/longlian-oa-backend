package online.longlian.app.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邀请码类型
 */
@Getter
@AllArgsConstructor
public enum InviteMode {

    /**
     * 超管邀请新用户注册并创建组织
     */
    SUPER_ADMIN_CREATE_ORG,

    /**
     * 组织管理员生成的组织邀请码
     */
    ORG_ADMIN_INVITE
}
