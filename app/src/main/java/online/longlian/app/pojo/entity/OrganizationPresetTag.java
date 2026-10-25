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
 * 组织预制标签表
 * </p>
 *
 * @author longlian
 * @since 2026-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("organization_preset_tag")
@ApiModel(value = "OrganizationPresetTag对象", description = "组织预制标签表")
public class OrganizationPresetTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 组织预制标签ID
     */
    @TableId("id")
    @ApiModelProperty("组织预制标签ID")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("所属组织ID")
    private Long orgId;

    /**
     * 标签名
     */
    @TableField("name")
    @ApiModelProperty("标签名")
    private String name;

    /**
     * 颜色，空为默认。blue/green/amber/rose/violet
     */
    @TableField("color")
    @ApiModelProperty("颜色，空为默认。blue/green/amber/rose/violet")
    private String color;

    /**
     * 优先级，仅用户端下拉按值降序
     */
    @TableField("priority")
    @ApiModelProperty("优先级，仅用户端下拉按值降序")
    private Integer priority;

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
