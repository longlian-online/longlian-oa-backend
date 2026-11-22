package online.longlian.app.pojo.bo.open;

/**
 * 开放面任务提交版本列表入参。组织上下文来自密钥，不接受调用方指定。
 */
public record OpenTaskSubmissionsParamsBO(Long instanceId, Long orgId) {
}
