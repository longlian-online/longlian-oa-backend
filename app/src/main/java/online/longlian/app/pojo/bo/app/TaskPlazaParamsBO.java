package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.PlazaSortBy;
import online.longlian.app.pojo.bo.common.PageParamsBO;

/**
 * 任务广场（缺人任务）查询入参。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPlazaParamsBO {

    private Long orgId;

    private String keyword;

    /** 企划类型名称，空表示不过滤 */
    private String projectType;

    private PlazaSortBy sortBy;

    private PageParamsBO page;
}
