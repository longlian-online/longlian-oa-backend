package online.longlian.app.pojo.bo.open;

/**
 * 开放面接取任务出参：仅保留调用方需要的自动入组标记，作为幂等快照内容。
 */
public record OpenTaskClaimResultBO(boolean autoJoinedProject) {
}
