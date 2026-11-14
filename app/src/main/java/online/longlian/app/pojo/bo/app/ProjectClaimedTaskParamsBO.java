package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectClaimedTaskParamsBO {
    private Long projectId;
    private Long assigneeId;
}
