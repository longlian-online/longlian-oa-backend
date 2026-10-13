package online.longlian.app.common.security;

/**
 * 本次请求声明的组织成员关系。只存在于 request attribute，不进入登录缓存。
 */
public record OrganizationScope(long orgId, long memberId, String orgRole) {
}
