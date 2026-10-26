package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstanceProxySubmitParamsBO {
    private Long instanceId;
    private Long userId;
    private Long orgId;
    private Long assigneeId;
    private LocalDateTime submittedAt;
    private String expectedRevision;
    private Map<String, Object> values;
}
