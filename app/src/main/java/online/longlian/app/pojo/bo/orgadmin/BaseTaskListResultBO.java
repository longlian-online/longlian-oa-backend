package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.Status;

import java.time.LocalDateTime;
import online.longlian.app.pojo.bo.common.TaskFormField;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseTaskListResultBO {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private String iconName;
    private List<TaskFormField> submitFields;
    private Integer refCount;
    private Status status;
    private LocalDateTime createdAt;
}
