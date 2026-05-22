package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTaskTemplateNodeCreateParamsBO {
    private Long baseTaskId;
    private String customName;
    private Integer sort;
    private Integer parallelSort;
}
