package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTypeCreateParamsBO {
    private Long orgId;
    private Long creatorId;
    private String name;
    private Integer priority;
    private List<ProjectTypeCustomFieldBO> customFields;
}
