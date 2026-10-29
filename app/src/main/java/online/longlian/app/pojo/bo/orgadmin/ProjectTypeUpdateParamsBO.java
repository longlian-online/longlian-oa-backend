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
public class ProjectTypeUpdateParamsBO {
    private Long typeId;
    private Long orgId;
    private String name;
    private Integer priority;
    private List<ProjectTypeCustomFieldBO> customFields;
}
