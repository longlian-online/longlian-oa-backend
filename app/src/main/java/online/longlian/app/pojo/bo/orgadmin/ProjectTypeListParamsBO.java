package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.PageParamsBO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTypeListParamsBO {
    private Long orgId;
    private String keyword;
    private SortDirection orderDir;
    private PageParamsBO page;
}
