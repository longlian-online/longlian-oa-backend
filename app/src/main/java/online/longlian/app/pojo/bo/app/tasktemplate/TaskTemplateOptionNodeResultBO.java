package online.longlian.app.pojo.bo.app.tasktemplate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.Status;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateOptionNodeResultBO {

    private Long baseTaskId;

    private String baseTaskName;

    private Status baseTaskStatus;

    private Integer sort;

    private Integer parallelSort;
}
