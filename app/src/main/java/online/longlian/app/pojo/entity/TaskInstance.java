package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务实例表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("task_instance")
@Schema(name = "TaskInstance", description = "任务实例表")
public class TaskInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    @Schema(description = "任务实例ID")
    private Long id;

    /**
     * 所属企划ID
     */
    @TableField("project_id")
    @Schema(description = "所属企划ID")
    private Long projectId;

    /**
     * 所属项目ID
     */
    @TableField("item_id")
    @Schema(description = "所属项目ID")
    private Long itemId;

    /**
     * 关联原子任务ID
     */
    @TableField("base_task_id")
    @Schema(description = "关联原子任务ID")
    private Long baseTaskId;

    /**
     * 关联任务模板ID
     */
    @TableField("task_template_id")
    @Schema(description = "关联任务模板ID")
    private Long taskTemplateId;

    /**
     * 接取人ID
     */
    @TableField("assignee_id")
    @Schema(description = "接取人ID")
    private Long assigneeId;

    /**
     * 任务状态 1-PENDING(待接取) 2-CLAIMED(已接取) 3-COMPLETED(已完成)
     */
    @TableField("status")
    @Schema(description = "任务状态 1-PENDING(待接取) 2-CLAIMED(已接取) 3-COMPLETED(已完成)")
    private Byte status;

    /**
     * 提交时间
     */
    @TableField("submitted_at")
    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}