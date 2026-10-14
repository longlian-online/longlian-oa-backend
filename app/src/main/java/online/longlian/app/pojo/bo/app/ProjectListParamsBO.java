package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.common.PageParamsBO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectListParamsBO {
    private Long orgId;
    private String keyword;
    private String projectType;
    private SortByTime sortByTime;
    private SortDirection orderDir;
    private PageParamsBO page;
    /** 显式分页起始偏移量；为 null 时走传统 pageNum 分页 */
    private Integer pageOffset;
}
