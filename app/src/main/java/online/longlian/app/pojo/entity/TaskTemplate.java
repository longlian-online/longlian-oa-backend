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
 * 任务模板表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("task_template")
@Schema(name = "TaskTemplate", description = "任务模板表")
public class TaskTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务模板ID
     */
    @TableId("id")
    @Schema(description = "任务模板ID")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @Schema(description = "所属组织ID")
    private Long orgId;

    /**
     * 任务模板名称
     */
    @TableField("name")
    @Schema(description = "任务模板名称")
    private String name;

    /**
     * 模板说明
     */
    @TableField("description")
    @Schema(description = "模板说明")
    private String description;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @Schema(description = "状态 1-启用 0-禁用")
    private Byte status;

    /**
     * 关联的项目数
     */
    @TableField("ref_count")
    @Schema(description = "关联的项目数")
    private Integer refCount;

    /**
     * 创建人ID
     */
    @TableField("creator_id")
    @Schema(description = "创建人ID")
    private Long creatorId;

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