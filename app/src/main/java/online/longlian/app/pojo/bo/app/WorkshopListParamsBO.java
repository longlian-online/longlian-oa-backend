package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.common.enumeration.WorkshopSortBy;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopListParamsBO {
    private Long userId;
    private Long orgId;
    private String keyword;
    private String projectType;
    private Boolean isMyCreated;
    private WorkshopSortBy sortBy;
    private PageParamsBO page;
}
