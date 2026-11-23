package online.longlian.app.pojo.bo.open;

/**
 * 创建 OpenAPI 密钥结果。key 明文只在创建链路出现一次，之后不可再取。
 */
public record OpenApiKeyCreateResultBO(Long id, String key) {
}
