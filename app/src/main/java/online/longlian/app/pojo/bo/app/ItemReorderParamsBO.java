package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemReorderParamsBO {
    private Long projectId;
    private Long orgId;
    private Long operatorId;
    private List<Long> expectedItemIds;
    private List<Long> itemIds;
}
