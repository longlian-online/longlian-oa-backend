package online.longlian.app.pojo.bo.common;

import lombok.Getter;

/**
 * 定时任务元数据定义，包含任务名称、cron 表达式、是否启用等信息。
 */
@Getter
public final class ScheduledTaskDefinition {

    private final String taskName;

    private final String description;

    /** cron 表达式，为 null 表示仅支持手动触发 */
    private final String cronExpression;

    private final boolean enabled;

    public ScheduledTaskDefinition(String taskName, String description, String cronExpression, boolean enabled) {
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("定时任务标识不能为空");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("定时任务必须填写描述，任务标识=" + taskName);
        }
        if (enabled && (cronExpression == null || cronExpression.isBlank())) {
            throw new IllegalArgumentException("启用自动调度的定时任务必须填写 Cron 表达式，任务标识=" + taskName);
        }
        this.taskName = taskName;
        this.description = description;
        this.cronExpression = cronExpression;
        this.enabled = enabled;
    }

    public String getLabel() {
        return description + "（" + taskName + "）";
    }
}
