package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
@Schema(name = "Permission", description = "权限表")
public class Permission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "权限ID")
    private Long id;

    /**
     * 权限编码 user:add
     */
    @TableField("perm_code")
    @Schema(description = "权限编码 user:add")
    private String permCode;

    /**
     * 权限名称
     */
    @TableField("perm_name")
    @Schema(description = "权限名称")
    private String permName;

    /**
     * 类型 1-菜单 2-按钮 3-接口
     */
    @TableField("perm_type")
    @Schema(description = "类型 1-菜单 2-按钮 3-接口")
    private Integer permType;

    /**
     * 前端路由 / 接口路径
     */
    @TableField("path")
    @Schema(description = "前端路由 / 接口路径")
    private String path;

    /**
     * 父权限ID
     */
    @TableField("parent_id")
    @Schema(description = "父权限ID")
    private Long parentId;

    /**
     * 排序
     */
    @TableField("sort")
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_at")
    @Schema(description = "创建时间")
    private LocalDateTime createAt;

    /**
     * 更新时间
     */
    @TableField("update_at")
    @Schema(description = "更新时间")
    private LocalDateTime updateAt;

    /**
     * 删除时间
     */
    @TableField("delete_at")
    @Schema(description = "删除时间")
    private LocalDateTime deleteAt;
}