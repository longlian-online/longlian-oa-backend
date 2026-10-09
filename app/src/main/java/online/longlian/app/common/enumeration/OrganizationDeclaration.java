package online.longlian.app.common.enumeration;

/**
 * 用户令牌接口是否读取 {@code X-Org-Id}。没有默认值，每个方法必须显式选择。
 */
public enum OrganizationDeclaration {

    /** 必须携带合法组织，并把本次权限替换成该组织角色。 */
    REQUIRED,

    /** 不读取组织头。头缺失或非法也不失败。 */
    NONE
}
