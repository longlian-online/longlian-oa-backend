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
     * 组织管理员邀请新用户注册并直接加入组织
     */
    ORG_ADMIN_REGISTER_JOIN,

    /**
     * 组织管理员邀请已注册用户加入组织
     */
    ORG_ADMIN_MEMBER_JOIN
}
