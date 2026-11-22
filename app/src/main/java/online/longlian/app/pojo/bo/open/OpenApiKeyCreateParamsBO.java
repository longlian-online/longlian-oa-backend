package online.longlian.app.pojo.bo.open;

/**
 * 创建 OpenAPI 密钥入参。创建和吊销都作用于当前组织。
 */
public record OpenApiKeyCreateParamsBO(Long userId, Long orgId, String name) {
}
