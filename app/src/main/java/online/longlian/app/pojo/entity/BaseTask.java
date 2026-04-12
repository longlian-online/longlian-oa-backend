package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;import online.longlian.app.common.enumeration.Status;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/**
 * <p>
 * 原子任务表（最小任务单元）
 * </p>
 *
 * @author longlian
 * @since 2026-04-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("base_task")
@ApiModel(value = "BaseTask对象", description = "原子任务表（最小任务单元）")
public class BaseTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 原子任务ID
     */
    @TableId("id")
    @ApiModelProperty("原子任务ID")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("所属组织ID")
    private Long orgId;

    /**
     * 任务名称（如：创建/翻译/校对）
     */
    @TableField("name")
    @ApiModelProperty("任务名称（如：创建/翻译/校对）")
    private String name;

    /**
     * 图标标识
     */
    @ApiModelProperty("图标标识")
    @TableField("icon_file_id")
    private Long iconFileId;

    /**
     * 任务说明
     */
    @ApiModelProperty("任务说明")
    @TableField("description")
    private String description;

    /**
     * 是否需要上传文件：0-否 1-是
     */
    @TableField("need_file")
    @ApiModelProperty("是否需要上传文件：0-否 1-是")
    private Byte needFile;

    /**
     * 是否必须上传：0-否 1-是
     */
    @TableField("required_file")
    @ApiModelProperty("是否必须上传：0-否 1-是")
    private Byte requiredFile;

    /**
     * 允许的文件类型（逗号分隔）
     */
    @TableField("allowed_mime_types")
    @ApiModelProperty("允许的文件类型（逗号分隔）")
    private String allowedMimeTypes;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @ApiModelProperty("状态 1-启用 0-禁用")
    private Status status;

    /**
     * 创建人ID
     */
    @TableField("creator_id")
    @ApiModelProperty("创建人ID")
    private Long creatorId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
