package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;import online.longlian.app.common.enumeration.TaskInstanceStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/**
 * <p>
 * 任务实例表
 * </p>
 *
 * @author longlian
 * @since 2026-04-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_instance")
@ApiModel(value = "TaskInstance对象", description = "任务实例表")
public class TaskInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    /**
     * 所属企划ID
     */
    @TableField("project_id")
    @ApiModelProperty("所属企划ID")
    private Long projectId;

    /**
     * 所属项目ID
     */
    @TableField("item_id")
    @ApiModelProperty("所属项目ID")
    private Long itemId;

    /**
     * 关联原子任务ID
     */
    @TableField("base_task_id")
    @ApiModelProperty("关联原子任务ID")
    private Long baseTaskId;

    /**
     * 关联任务模板ID
     */
    @ApiModelProperty("关联任务模板ID")
    @TableField("task_template_id")
    private Long taskTemplateId;

    /**
     * 接取人ID
     */
    @ApiModelProperty("接取人ID")
    @TableField("assignee_id")
    private Long assigneeId;

    /**
     * 任务状态 1-PENDING(待接取) 2-CLAIMED(已接取) 3-COMPLETED(已完成)
     */
    @TableField("status")
    @ApiModelProperty("任务状态 1-PENDING(待接取) 2-CLAIMED(已接取) 3-COMPLETED(已完成)")
    private TaskInstanceStatus status;

    /**
     * 提交时间
     */
    @ApiModelProperty("提交时间")
    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    /**
     * 完成时间
     */
    @ApiModelProperty("完成时间")
    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
