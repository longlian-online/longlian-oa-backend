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
import online.longlian.common.enumeration.TaskInstanceStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务实例表
 * </p>
 *
 * @author longlian
 * @since 2026-04-19
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
     * 关联项目任务节点ID
     */
    @ApiModelProperty("关联项目任务节点ID")
    @TableField("item_task_node_id")
    private Long itemTaskNodeId;

    /**
     * 关联任务流ID
     */
    @TableField("task_flow_id")
    @ApiModelProperty("关联任务流ID")
    private Long taskFlowId;

    /**
     * 接取人ID
     */
    @ApiModelProperty("接取人ID")
    @TableField("assignee_id")
    private Long assigneeId;

    /**
     * 任务状态 1-PENDING(待接取) 2-CLAIMED(待提交) 3-COMPLETED(已完成)
     */
    @TableField("status")
    @ApiModelProperty("任务状态 1-PENDING(待接取) 2-CLAIMED(待提交) 3-COMPLETED(已完成)")
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
