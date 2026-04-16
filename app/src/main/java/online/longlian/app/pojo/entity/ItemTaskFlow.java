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

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 项目任务流表
 * </p>
 *
 * @author longlian
 * @since 2026-04-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("item_task_flow")
@ApiModel(value = "ItemTaskFlow对象", description = "项目任务流表")
public class ItemTaskFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目任务流ID
     */
    @TableId("id")
    @ApiModelProperty("项目任务流ID")
    private Long id;

    /**
     * 所属项目ID
     */
    @TableField("item_id")
    @ApiModelProperty("所属项目ID")
    private Long itemId;

    /**
     * 所属企划ID
     */
    @TableField("project_id")
    @ApiModelProperty("所属企划ID")
    private Long projectId;

    /**
     * 关联任务模板ID
     */
    @ApiModelProperty("关联任务模板ID")
    @TableField("task_template_id")
    private Long taskTemplateId;

    /**
     * 任务流名称（继承自任务模板）
     */
    @TableField("name")
    @ApiModelProperty("任务流名称（继承自任务模板）")
    private String name;

    /**
     * 任务流说明
     */
    @ApiModelProperty("任务流说明")
    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
