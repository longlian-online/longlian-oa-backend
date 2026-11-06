package online.longlian.app.pojo.bo.app;

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
public class TaskDraftResultBO {

    private String instanceId;
    private String itemId;
    private String taskRevision;
    private boolean canSubmit;
    private List<TaskFormField> submitFields;
    private TaskDraftContentResultBO draft;
}
