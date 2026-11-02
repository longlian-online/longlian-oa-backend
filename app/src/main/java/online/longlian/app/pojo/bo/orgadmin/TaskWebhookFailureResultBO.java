package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TaskWebhookFailureResultBO {
    Long id;
    String event;
    Long taskInstanceId;
    Long submissionId;
    String lastError;
    Integer attemptCount;
    LocalDateTime updatedAt;
}
