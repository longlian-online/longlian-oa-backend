package online.longlian.app.pojo.bo.app.taskinstance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstanceListParamsBO {
    private Long itemId;
    private Long userId;
    private Long orgId;
}
