package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.ItemStatus;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.PageParamsBO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemListParamsBO {
    private Long projectId;
    private String keyword;
    private ItemStatus status;
    private SortByTime sortByTime;
    private SortDirection orderDir;
    private PageParamsBO page;
}
