package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/**
 * <p>
 * 任务模板节点表
 * </p>
 *
 * @author longlian
 * @since 2026-04-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_template_node")
@ApiModel(value = "TaskTemplateNode对象", description = "任务模板节点表")
public class TaskTemplateNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务模板节点ID
     */
    @TableId("id")
    @ApiModelProperty("任务模板节点ID")
    private Long id;

    /**
     * 所属任务模板ID
     */
    @ApiModelProperty("所属任务模板ID")
    @TableField("task_template_id")
    private Long taskTemplateId;

    /**
     * 关联原子任务ID
     */
    @TableField("base_task_id")
    @ApiModelProperty("关联原子任务ID")
    private Long baseTaskId;

    /**
     * 步骤顺序
     */
    @TableField("sort")
    @ApiModelProperty("步骤顺序")
    private Integer sort;

    /**
     * 并行组号排序
     */
    @ApiModelProperty("并行组号排序")
    @TableField("parallel_sort")
    private Integer parallelSort;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
