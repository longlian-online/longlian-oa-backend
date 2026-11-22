package online.longlian.app.pojo.bo.open;

/**
 * 吊销 OpenAPI 密钥入参。密钥必须属于当前用户在当前组织的密钥。
 */
public record OpenApiKeyRevokeParamsBO(Long userId, Long orgId, Long keyId) {
}
