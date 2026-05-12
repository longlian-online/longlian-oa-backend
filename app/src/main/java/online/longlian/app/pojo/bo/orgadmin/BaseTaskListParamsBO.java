package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.BaseTaskSortBy;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.PageParamsBO;
import online.longlian.common.enumeration.Status;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseTaskListParamsBO {
    private Long orgId;
    private String keyword;
    private Status status;
    private LocalDateTime startCreatedTime;
    private LocalDateTime endCreatedTime;
    private BaseTaskSortBy sortBy;
    private SortDirection orderDir;
    private PageParamsBO page;
}
