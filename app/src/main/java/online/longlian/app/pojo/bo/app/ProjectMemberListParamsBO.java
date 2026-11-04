package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Data;
import online.longlian.app.pojo.bo.common.PageParamsBO;

@Data
@Builder
public class ProjectMemberListParamsBO {
    private Long projectId;
    private Long orgId;
    private PageParamsBO page;
}
