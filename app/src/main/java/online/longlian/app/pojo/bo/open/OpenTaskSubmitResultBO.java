package online.longlian.app.pojo.bo.open;

/**
 * 开放面提交任务出参：本次提交的版本号，作为幂等快照内容。
 */
public record OpenTaskSubmitResultBO(int versionNo) {
}
