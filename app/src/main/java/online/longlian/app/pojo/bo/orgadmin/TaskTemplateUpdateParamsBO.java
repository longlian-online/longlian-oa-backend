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
public class TaskTemplateUpdateParamsBO {
    private Long templateId;
    private Long orgId;
    private String name;
    private String description;
    private List<TaskTemplateNodeCreateParamsBO> nodes;
}
