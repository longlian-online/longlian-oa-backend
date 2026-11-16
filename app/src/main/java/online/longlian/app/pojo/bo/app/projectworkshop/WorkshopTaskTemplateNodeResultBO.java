package online.longlian.app.pojo.bo.app.projectworkshop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTaskTemplateNodeResultBO {

    private Long baseTaskId;

    private String baseTaskName;

    private String baseTaskIcon;

    private String customName;

    private Integer sort;

    private Integer parallelSort;
}
