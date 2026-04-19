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
import online.longlian.generator.enumeration.Status;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 原子任务表（最小任务单元）
 * </p>
 *
 * @author longlian
 * @since 2026-04-18
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
     * 任务名称（标题）（如：创建/翻译/校对）
     */
    @TableField("name")
    @ApiModelProperty("任务名称（标题）（如：创建/翻译/校对）")
    private String name;

    /**
     * 图标标识
     */
    @ApiModelProperty("图标标识")
    @TableField("icon_file_id")
    private Long iconFileId;

    /**
     * 任务说明（简介）
     */
    @TableField("description")
    @ApiModelProperty("任务说明（简介）")
    private String description;

    /**
     * 元数据字段定义(JSON数组)
     */
    @TableField("meta_schema")
    @ApiModelProperty("元数据字段定义(JSON数组)")
    private String metaSchema;

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
