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
 * 项目表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("item")
@Schema(name = "Item", description = "项目表")
public class Item implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    @Schema(description = "项目ID")
    private Long id;

    /**
     * 所属企划ID
     */
    @TableField("project_id")
    @Schema(description = "所属企划ID")
    private Long projectId;

    /**
     * 项目名称
     */
    @TableField("title")
    @Schema(description = "项目名称")
    private String title;

    /**
     * 创建时关联的任务模板ID（仅记录溯源）
     */
    @TableField("task_template_id")
    @Schema(description = "创建时关联的任务模板ID（仅记录溯源）")
    private Long taskTemplateId;

    /**
     * 状态 1-进行中 2-已完成 3-待发布
     */
    @TableField("status")
    @Schema(description = "状态 1-进行中 2-已完成 3-待发布")
    private Byte status;

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