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
 * 项目任务流表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("item_task_flow")
@Schema(name = "ItemTaskFlow", description = "项目任务流表")
public class ItemTaskFlow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 项目任务流ID
     */
    @TableId("id")
    @Schema(description = "项目任务流ID")
    private Long id;

    /**
     * 所属项目ID
     */
    @TableField("item_id")
    @Schema(description = "所属项目ID")
    private Long itemId;

    /**
     * 所属企划ID
     */
    @TableField("project_id")
    @Schema(description = "所属企划ID")
    private Long projectId;

    /**
     * 关联任务模板ID
     */
    @TableField("task_template_id")
    @Schema(description = "关联任务模板ID")
    private Long taskTemplateId;

    /**
     * 任务流名称（继承自任务模板）
     */
    @TableField("name")
    @Schema(description = "任务流名称（继承自任务模板）")
    private String name;

    /**
     * 任务流说明
     */
    @TableField("description")
    @Schema(description = "任务流说明")
    private String description;

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