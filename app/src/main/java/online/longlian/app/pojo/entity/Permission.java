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
import online.longlian.generator.enumeration.PermissionType;
import online.longlian.generator.enumeration.Status;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 权限表
 * </p>
 *
 * @author longlian
 * @since 2026-04-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("permission")
@ApiModel(value = "Permission对象", description = "权限表")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    @TableId("id")
    @ApiModelProperty("权限ID")
    private Long id;

    /**
     * 权限编码（如：org:member:disable）
     */
    @TableField("perm_code")
    @ApiModelProperty("权限编码（如：org:member:disable）")
    private String permCode;

    /**
     * 权限名称
     */
    @TableField("perm_name")
    @ApiModelProperty("权限名称")
    private String permName;

    /**
     * 类型 1-菜单 2-按钮 3-接口
     */
    @TableField("perm_type")
    @ApiModelProperty("类型 1-菜单 2-按钮 3-接口")
    private PermissionType permType;

    /**
     * 前端路由/接口路径
     */
    @TableField("path")
    @ApiModelProperty("前端路由/接口路径")
    private String path;

    /**
     * 父权限ID（用于菜单层级）
     */
    @TableField("parent_id")
    @ApiModelProperty("父权限ID（用于菜单层级）")
    private Long parentId;

    /**
     * 排序（前端展示顺序）
     */
    @TableField("sort")
    @ApiModelProperty("排序（前端展示顺序）")
    private Integer sort;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @ApiModelProperty("状态 1-启用 0-禁用")
    private Status status;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    @TableField("deleted_at")
    @ApiModelProperty("软删除时间")
    private LocalDateTime deletedAt;
}
