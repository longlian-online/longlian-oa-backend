package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.generator.enumeration.ScheduledTaskStatus;
import online.longlian.generator.enumeration.TriggerSource;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 定时任务执行日志表
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("scheduled_task_log")
@ApiModel(value = "ScheduledTaskLog对象", description = "定时任务执行日志表")
public class ScheduledTaskLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId("id")
    @ApiModelProperty("日志ID")
    private Long id;

    /**
     * 任务名称
     */
    @TableField("task_name")
    @ApiModelProperty("任务名称")
    private String taskName;

    /**
     * 触发来源 1-SCHEDULED(定时触发) 2-MANUAL(手动触发)
     */
    @TableField("trigger_source")
    @ApiModelProperty("触发来源 1-SCHEDULED(定时触发) 2-MANUAL(手动触发)")
    private TriggerSource triggerSource;

    /**
     * 上层传递的执行时间
     */
    @ApiModelProperty("上层传递的执行时间")
    @TableField("execute_time_param")
    private LocalDateTime executeTimeParam;

    /**
     * 执行状态 1-RUNNING(执行中) 2-SUCCESS(成功) 3-FAILED(失败)
     */
    @TableField("status")
    @ApiModelProperty("执行状态 1-RUNNING(执行中) 2-SUCCESS(成功) 3-FAILED(失败)")
    private ScheduledTaskStatus status;

    /**
     * 失败时的错误信息
     */
    @TableField("error_message")
    @ApiModelProperty("失败时的错误信息")
    private String errorMessage;

    /**
     * 执行追踪ID
     */
    @ApiModelProperty("执行追踪ID")
    @TableField("execution_id")
    private String executionId;

    /**
     * 手动触发人ID（定时触发时为NULL）
     */
    @TableField("triggered_by")
    @ApiModelProperty("手动触发人ID（定时触发时为NULL）")
    private Long triggeredBy;

    /**
     * 开始执行时间
     */
    @TableField("started_at")
    @ApiModelProperty("开始执行时间")
    private LocalDateTime startedAt;

    /**
     * 结束执行时间
     */
    @TableField("ended_at")
    @ApiModelProperty("结束执行时间")
    private LocalDateTime endedAt;

    /**
     * 执行耗时（毫秒）
     */
    @TableField("duration_ms")
    @ApiModelProperty("执行耗时（毫秒）")
    private Long durationMs;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    @TableField("deleted_at")
    @ApiModelProperty("软删除时间")
    private LocalDateTime deletedAt;
}
