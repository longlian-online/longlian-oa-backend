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
public class TaskTemplateNodeResultBO {
    private Long id;
    private Long baseTaskId;
    private String baseTaskName;
    private String baseTaskIcon;
    private List<TaskFormField> submitFields;
    private Integer sort;
    private Integer parallelSort;
}
