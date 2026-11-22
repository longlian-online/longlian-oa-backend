package online.longlian.app.common.security;

/**
 * 开放接口认证主体：密钥解出的用户与组织。组织上下文只来自密钥，不来自请求头。
 * 刻意不是 UserDetailImpl：OrganizationScopeInterceptor 对非 UserDetailImpl 主体短路，
 * /open/** 因此不走 X-Org-Id 授权路径。权限标识常量在 OpenApiConstants。
 */
public record OpenApiPrincipal(Long keyId, Long userId, Long orgId) {
}
