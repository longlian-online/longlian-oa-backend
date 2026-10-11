package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.PlazaSortBy;

/**
 * 任务广场自定义 SQL 查询参数（关键词已预处理为 LIKE 匹配串）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPlazaSearchParamsBO {

    private Long orgId;

    /** 企划类型ID，空表示不过滤 */
    private Long typeId;

    /** LIKE 匹配串（%/_/! 已转义），空表示不过滤 */
    private String matchPattern;

    private PlazaSortBy sortBy;
}
