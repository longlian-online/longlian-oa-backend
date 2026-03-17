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
 * 原子任务表（最小任务单元）
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("base_task")
@Schema(name = "BaseTask", description = "原子任务表（最小任务单元）")
public class BaseTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 原子任务ID
     */
    @TableId("id")
    @Schema(description = "原子任务ID")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @Schema(description = "所属组织ID")
    private Long orgId;

    /**
     * 任务名称（如：创建/翻译/校对）
     */
    @TableField("name")
    @Schema(description = "任务名称（如：创建/翻译/校对）")
    private String name;

    /**
     * 图标标识
     */
    @TableField("icon_file_id")
    @Schema(description = "图标标识")
    private Long iconFileId;

    /**
     * 任务说明
     */
    @TableField("description")
    @Schema(description = "任务说明")
    private String description;

    /**
     * 是否需要上传文件：0-否 1-是
     */
    @TableField("need_file")
    @Schema(description = "是否需要上传文件：0-否 1-是")
    private Byte needFile;

    /**
     * 是否必须上传：0-否 1-是
     */
    @TableField("required_file")
    @Schema(description = "是否必须上传：0-否 1-是")
    private Byte requiredFile;

    /**
     * 允许的文件类型（逗号分隔）
     */
    @TableField("allowed_mime_types")
    @Schema(description = "允许的文件类型（逗号分隔）")
    private String allowedMimeTypes;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @Schema(description = "状态 1-启用 0-禁用")
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