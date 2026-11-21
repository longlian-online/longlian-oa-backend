package online.longlian.app.service.open;

import online.longlian.app.pojo.bo.open.OpenApiKeyAuthResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyCreateParamsBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyCreateResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyGroupResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyRevokeParamsBO;

import java.util.List;

/**
 * OpenAPI 密钥：与 (用户, 组织) 绑定的长期凭证。明文只在创建时返回一次，服务端只存散列。
 */
public interface OpenApiKeyService {

    /**
     * 校验密钥并解出用户与组织。密钥不存在、已吊销，或用户已不在该组织/被停用时抛
     * {@link online.longlian.app.common.exception.OpenAppException}（10001）。
     */
    OpenApiKeyAuthResultBO authenticate(String plaintextKey);

    /** 异步更新最近使用时间，不阻塞请求。 */
    void touchLastUsedAsync(Long keyId);

    /** 在当前组织创建密钥，超出 (user, org) 启用上限时拒绝。 */
    OpenApiKeyCreateResultBO create(OpenApiKeyCreateParamsBO params);

    /** 按组织分组列出该用户全部可用组织的密钥。 */
    List<OpenApiKeyGroupResultBO> listGroups(Long userId);

    /** 吊销密钥，立即生效；行保留审计。 */
    void revoke(OpenApiKeyRevokeParamsBO params);

    /** 吊销该用户全部组织的全部密钥（密码修改/重置后调用）。 */
    void revokeAllForUser(Long userId);
}
