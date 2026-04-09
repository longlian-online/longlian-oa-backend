package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import online.longlian.app.common.enumeration.Status;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
@Getter
@Setter
@ToString
@TableName("role")
@Schema(name = "Role", description = "角色表")
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "角色ID")
    private Long id;

    /**
     * 角色编码（ADMIN / USER）
     */
    @TableField("role_code")
    @Schema(description = "角色编码（ADMIN / USER）")
    private String roleCode;

    /**
     * 角色名称
     */
    @TableField("role_name")
    @Schema(description = "角色名称")
    private String roleName;

    /**
     * 角色描述
     */
    @TableField("description")
    @Schema(description = "角色描述")
    private String description;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @Schema(description = "状态 1-启用 0-禁用")
    private Status status;

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
