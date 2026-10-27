package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstanceAssignParamsBO {
    private Long instanceId;
    private Long userId;
    private Long orgId;
    private Long assigneeId;
    private String expectedRevision;
}
