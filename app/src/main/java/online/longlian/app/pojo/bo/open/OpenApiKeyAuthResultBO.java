package online.longlian.app.pojo.bo.open;

/**
 * 密钥认证结果：密钥解出的用户与组织。
 */
public record OpenApiKeyAuthResultBO(Long keyId, Long userId, Long orgId) {
}
