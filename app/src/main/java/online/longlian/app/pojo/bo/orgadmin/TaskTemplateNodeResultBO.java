package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateNodeResultBO {
    private Long id;
    private Long baseTaskId;
    private String baseTaskName;
    private String baseTaskIconUrl;
    private String metaSchema;
    private Integer sort;
    private Integer parallelSort;
}
