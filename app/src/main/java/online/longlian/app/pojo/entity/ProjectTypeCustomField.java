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
 * 企划类型默认自定义表单
 * </p>
 *
 * @author longlian
 * @since 2026-10-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("project_type_custom_field")
@ApiModel(value = "ProjectTypeCustomField对象", description = "企划类型默认自定义表单")
public class ProjectTypeCustomField implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    @ApiModelProperty("企划类型默认自定义字段ID")
    private Long id;

    @TableField("org_id")
    @ApiModelProperty("所属组织ID")
    private Long orgId;

    @TableField("type_id")
    @ApiModelProperty("企划类型ID")
    private Long typeId;

    @TableField("preset_field_id")
    @ApiModelProperty("预置表单字段ID")
    private Long presetFieldId;

    @TableField("required")
    @ApiModelProperty("是否必填 1-必填 0-选填")
    private Boolean required;

    @TableField("sort")
    @ApiModelProperty("表单内顺序，从 0 开始")
    private Integer sort;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
