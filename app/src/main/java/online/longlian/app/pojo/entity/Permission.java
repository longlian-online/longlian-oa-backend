package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 权限表
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
@Getter
@Setter
@ToString
@TableName("permission")
@ApiModel(value = "Permission对象", description = "权限表")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    @ApiModelProperty("权限ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 权限编码 user:add
     */
    @TableField("perm_code")
    @ApiModelProperty("权限编码 user:add")
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
    private Integer permType;

    /**
     * 前端路由 / 接口路径
     */
    @TableField("path")
    @ApiModelProperty("前端路由 / 接口路径")
    private String path;

    /**
     * 父权限ID
     */
    @TableField("parent_id")
    @ApiModelProperty("父权限ID")
    private Long parentId;

    /**
     * 排序
     */
    @TableField("sort")
    @ApiModelProperty("排序")
    private Integer sort;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @ApiModelProperty("状态 1-启用 0-禁用")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_at")
    @ApiModelProperty("创建时间")
    private LocalDateTime createAt;

    /**
     * 更新时间
     */
    @TableField("update_at")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateAt;

    /**
     * 删除时间
     */
    @TableField("delete_at")
    @ApiModelProperty("删除时间")
    private LocalDateTime deleteAt;
}
