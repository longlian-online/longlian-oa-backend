package online.longlian.app.pojo.bo.open;

import java.time.LocalDateTime;

/**
 * OpenAPI 密钥条目（不含明文）。
 */
public record OpenApiKeyItemResultBO(Long id, String name, LocalDateTime createdAt,
                                     LocalDateTime lastUsedAt) {
}
