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
 * 用户角色关联表
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
@Getter
@Setter
@ToString
@TableName("user_role")
@Schema(name = "UserRole", description = "用户角色关联表")
public class UserRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 角色ID
     */
    @TableField("role_id")
    @Schema(description = "角色ID")
    private Long roleId;

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