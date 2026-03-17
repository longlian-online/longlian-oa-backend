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
 * 任务模板节点表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("task_template_node")
@Schema(name = "TaskTemplateNode", description = "任务模板节点表")
public class TaskTemplateNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务模板节点ID
     */
    @TableId("id")
    @Schema(description = "任务模板节点ID")
    private Long id;

    /**
     * 所属任务模板ID
     */
    @TableField("task_template_id")
    @Schema(description = "所属任务模板ID")
    private Long taskTemplateId;

    /**
     * 关联原子任务ID
     */
    @TableField("base_task_id")
    @Schema(description = "关联原子任务ID")
    private Long baseTaskId;

    /**
     * 步骤顺序
     */
    @TableField("sort")
    @Schema(description = "步骤顺序")
    private Integer sort;

    /**
     * 并行组号排序
     */
    @TableField("parallel_sort")
    @Schema(description = "并行组号排序")
    private Integer parallelSort;

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