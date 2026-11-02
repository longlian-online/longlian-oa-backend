package online.longlian.app.service.app.impl.taskwebhook;

/** 地址暂时解析不了，或对方网络失败。outbox 会按原有退避重试。 */
public class TaskWebhookTransientException extends RuntimeException {
    public TaskWebhookTransientException(String message) {
        super(message);
    }
}
