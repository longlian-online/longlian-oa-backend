package online.longlian.app.pojo.bo.open;

/**
 * 幂等查重结果信封：查重结论与命中时的首次快照数据。
 */
public record IdempotencyCheckResultBO(IdempotencyCheckOutcome outcome, Object data) {
}
