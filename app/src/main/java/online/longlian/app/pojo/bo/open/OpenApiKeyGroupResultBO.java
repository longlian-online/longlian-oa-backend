package online.longlian.app.pojo.bo.open;

import java.util.List;

/**
 * 按组织分组的 OpenAPI 密钥。只含用户当前可用组织。
 */
public record OpenApiKeyGroupResultBO(Long orgId, String orgName, Integer maxKeys,
                                      List<OpenApiKeyItemResultBO> keys) {
}
