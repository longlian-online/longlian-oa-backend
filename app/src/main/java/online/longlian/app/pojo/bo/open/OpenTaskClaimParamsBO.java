package online.longlian.app.pojo.bo.open;

/**
 * 开放面接取任务入参。用户与组织来自密钥解出的 principal。
 */
public record OpenTaskClaimParamsBO(Long instanceId, Long userId, Long orgId) {
}
