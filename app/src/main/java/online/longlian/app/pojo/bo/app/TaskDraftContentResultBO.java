package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.pojo.bo.common.TaskDraftFileResultBO;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDraftContentResultBO {

    private String id;
    private String revision;
    private LocalDateTime updatedAt;
    private Map<String, Object> values;
    private Map<String, TaskDraftFileResultBO> files;
}
