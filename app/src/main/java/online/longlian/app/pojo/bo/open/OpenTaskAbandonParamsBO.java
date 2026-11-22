package online.longlian.app.pojo.bo.open;

/**
 * 开放面放弃任务入参。expectedRevision 为任务当前修订号，开放面用数值表达，站内语义不变。
 */
public record OpenTaskAbandonParamsBO(Long instanceId, Long userId, Long orgId, Long expectedRevision) {
}
