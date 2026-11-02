package online.longlian.app.service.app.impl.taskwebhook;

import java.net.URI;

public interface TaskWebhookClient {
    /** @return HTTP 状态码。网络失败抛出 {@link TaskWebhookTransientException}，不返回对方正文。 */
    int post(URI target, byte[] body, String timestamp, String deliveryId, String signature);
}
