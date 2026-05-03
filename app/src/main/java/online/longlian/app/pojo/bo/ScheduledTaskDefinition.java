package online.longlian.app.pojo.bo;

import lombok.Builder;
import lombok.Data;

/**
 * 定时任务元数据定义，包含任务名称、cron 表达式、是否启用等信息。
 */
@Data
@Builder
public class ScheduledTaskDefinition {

    private String taskName;

    private String description;

    /** cron 表达式，为 null 表示仅支持手动触发 */
    private String cronExpression;

    private boolean enabled;
}
