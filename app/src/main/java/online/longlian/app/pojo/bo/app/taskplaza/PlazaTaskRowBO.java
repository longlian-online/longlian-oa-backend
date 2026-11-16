package online.longlian.app.pojo.bo.app.taskplaza;

import lombok.Data;

import java.time.LocalDateTime;
import online.longlian.common.enumeration.TaskInstanceStatus;

/**
 * 任务广场任务行，包含阶段标记及已占用槽位，用于并行槽统计。
 */
@Data
public class PlazaTaskRowBO {

    private Long instanceId;

    private Long projectId;

    private Long itemId;

    private String itemTitle;

    private String nodeName;

    private Long nodeId;

    private Long baseTaskId;

    /** 原子任务当前图标（Lucide 组件名），活读不快照；原子任务缺失时为 null */
    private String baseTaskIcon;

    private Integer sort;

    private Integer parallelSort;

    private TaskInstanceStatus status;

    private LocalDateTime createdAt;

    private Boolean isCurrentStage;
}
