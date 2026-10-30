package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.pojo.bo.common.TaskFormField;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseTaskUpdateParamsBO {
    private Long taskId;
    private Long orgId;
    private String name;
    private String description;
    private String icon;
    private List<TaskFormField> submitFields;
    private String webhookUrl;
    private String webhookSecret;
}
