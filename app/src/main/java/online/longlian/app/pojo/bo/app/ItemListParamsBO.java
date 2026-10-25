package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.ItemStatus;
import online.longlian.app.common.enumeration.ItemSortBy;
import online.longlian.app.pojo.bo.common.PageParamsBO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemListParamsBO {
    private Long projectId;
    private Long orgId;
    private String keyword;
    private ItemStatus status;
    private ItemSortBy sortBy;
    private PageParamsBO page;
}
