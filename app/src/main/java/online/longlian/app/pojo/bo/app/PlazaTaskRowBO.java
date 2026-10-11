package online.longlian.app.pojo.bo.app;

import lombok.Data;

import java.time.LocalDateTime;
import online.longlian.common.enumeration.TaskInstanceStatus;

/**
 * 任务广场任务行（自定义 SQL 结果）：当前阶段组内的任务实例（含已占用槽位，用于并行槽统计）。
 */
@Data
public class PlazaTaskRowBO {

    private Long instanceId;

    private Long projectId;

    private Long itemId;

    private String itemTitle;

    private String nodeName;

    private Integer sort;

    private Integer parallelSort;

    private TaskInstanceStatus status;

    private LocalDateTime createdAt;
}
