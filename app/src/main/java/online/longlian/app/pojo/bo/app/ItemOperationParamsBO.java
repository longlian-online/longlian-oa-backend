package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemOperationParamsBO {
    private Long projectId;
    private Long itemId;
    private Long orgId;
    private Long operatorId;
}
