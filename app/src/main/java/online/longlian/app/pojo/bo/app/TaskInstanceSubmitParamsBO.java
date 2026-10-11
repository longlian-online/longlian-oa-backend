package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstanceSubmitParamsBO {
    private Long instanceId;
    private Long userId;
    private Long orgId;
    private Map<String, Object> values;
}
