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
import online.longlian.generator.enumeration.TaskTemplateScope;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务模板表
 * </p>
 *
 * @author longlian
 * @since 2026-04-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_template")
@ApiModel(value = "TaskTemplate对象", description = "任务模板表")
public class TaskTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务模板ID
     */
    @TableId("id")
    @ApiModelProperty("任务模板ID")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("所属组织ID")
    private Long orgId;

    /**
     * 任务模板名称
     */
    @TableField("name")
    @ApiModelProperty("任务模板名称")
    private String name;

    /**
     * 模板说明
     */
    @ApiModelProperty("模板说明")
    @TableField("description")
    private String description;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @ApiModelProperty("状态 1-启用 0-禁用")
    private Status status;

    /**
     * 1-个人模板 2-组织通用模板
     */
    @TableField("scope")
    @ApiModelProperty("1-个人模板 2-组织通用模板")
    private TaskTemplateScope scope;

    /**
     * 关联的项目数
     */
    @TableField("ref_count")
    @ApiModelProperty("关联的项目数")
    private Integer refCount;

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
