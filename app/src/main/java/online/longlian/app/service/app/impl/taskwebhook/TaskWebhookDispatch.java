package online.longlian.app.service.app.impl.taskwebhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** outbox 里只放定位信息。地址、密钥和签名链接都在投递时现取，避免密钥进出发件表。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskWebhookDispatch(
        long orgId,
        String event,
        long taskInstanceId,
        long submissionId,
        long itemTaskNodeId,
        long revision,
        String occurredAt,
        String reason) {
}
