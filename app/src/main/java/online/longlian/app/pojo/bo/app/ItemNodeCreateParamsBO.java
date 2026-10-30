package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemNodeCreateParamsBO {
    private Long baseTaskId;
    private Integer sort;
    private Integer parallelSort;
    private String name;
    private Long assigneeId;
}
