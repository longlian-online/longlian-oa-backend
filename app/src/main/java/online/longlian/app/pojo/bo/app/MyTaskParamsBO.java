package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Data;
import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.common.PageParamsBO;

@Data
@Builder
public class MyTaskParamsBO {
    private Long userId;
    private Long orgId;
    private MyTaskBucket bucket;
    private SortDirection sortDirection;
    private PageParamsBO page;
}
