package online.longlian.app.pojo.bo.app;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务广场企划聚合行（自定义 SQL 结果）：一个企划的缺人任务汇总。
 */
@Data
public class PlazaProjectSummaryBO {

    private Long projectId;

    private String title;

    private String typeName;

    private Long coverFileId;

    /** 缺人（可接取）任务数 */
    private Long pendingCount;

    /** 涉及条目数 */
    private Long itemCount;

    /** 组内最早创建的缺人任务时间 */
    private LocalDateTime oldestCreatedAt;

    /** 组内最新创建的缺人任务时间 */
    private LocalDateTime latestCreatedAt;
}
