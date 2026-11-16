package online.longlian.app.pojo.bo.app.itemtaskflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.pojo.bo.common.TaskFormFieldBO;
import online.longlian.common.enumeration.TaskInstanceStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemTaskNodeResultBO {

    private Long id;

    private Long baseTaskId;

    private String name;

    private String baseTaskIcon;

    private List<TaskFormFieldBO> submitFields;

    private Integer sort;

    private Integer parallelSort;

    private Long taskInstanceId;

    private TaskInstanceStatus taskStatus;

    private String revision;

    private Boolean removable;
}
