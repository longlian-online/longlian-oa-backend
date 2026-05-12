package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.PageParamsBO;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAdminListParamsBO {
    private Long orgId;
    private String keyword;
    private Long typeId;
    private LocalDateTime startCreatedTime;
    private LocalDateTime endCreatedTime;
    private SortDirection orderDir;
    private PageParamsBO page;
}
