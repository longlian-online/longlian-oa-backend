package online.longlian.app.service;

import online.longlian.common.enumeration.TokenType;

/**
 * Token 黑名单服务接口。
 * <p>
 * 负责 JWT Token 的吊销管理，以数据库表作为黑名单存储，
 * 通过 {@code expiredAt} 时间戳字段实现 TTL 过期：
 * <ol>
 *   <li><b>单 Token 黑名单</b>：登出时将当前 Token 加入黑名单，有效期对齐 Token 剩余有效时间</li>
 *   <li><b>按用户全量拉黑</b>：删除管理员账号等场景下，将该用户所有 Token 批量加入黑名单，
 *       记录 key 格式为 {@code {tokenType}:user:{userId}:all}</li>
 *   <li><b>黑名单校验</b>：请求鉴权时检查 Token 是否在黑名单中且未过期，
 *       同时检查用户级别的全局黑名单标记</li>
 * </ol>
 */
public interface TokenBlacklistService {

    /**
     * 将 Token 加入黑名单。
     *
     * @param token        待加入黑名单的 JWT Token
     * @param tokenType    Token 类型（用户/管理员）
     * @param userId       关联的用户 ID
     * @param reason       加入黑名单原因（用于日志审计）
     * @param expireSeconds 黑名单过期秒数（通常对齐 Token 剩余有效期）
     */
    void addToBlacklist(String token, TokenType tokenType, Long userId, String reason, long expireSeconds);

    /**
     * 检查 Token 是否在黑名单中。
     * <p>
     * 同时检查两点：该 Token 本身是否被单独拉黑、该 Token 所属用户是否被全局拉黑。
     *
     * @param token JWT Token
     * @return true 表示已拉黑（请求应被拒绝）
     */
    boolean isBlacklisted(String token);

    /**
     * 从黑名单中移除指定 Token。
     *
     * @param token JWT Token
     */
    void removeFromBlacklist(String token);

    /**
     * 将某用户的所有 Token 批量加入黑名单（全局拉黑）。
     * <p>
     * 典型场景：管理员账号被删除时，立即吊销其名下所有 Token。
     *
     * @param tokenType Token 类型（用户/管理员）
     * @param userId    用户 ID
     * @param reason    拉黑原因
     */
    void blacklistAllUserTokens(TokenType tokenType, Long userId, String reason);
}
