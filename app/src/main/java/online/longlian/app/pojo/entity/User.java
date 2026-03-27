package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统用户表
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
@Getter
@Setter
@ToString
@TableName("user")
@Schema(name = "User", description = "系统用户表")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "用户ID")
    private Long id;

    /**
     * 用户名
     */
    @TableField("username")
    @Schema(description = "用户名")
    private String username;

    /**
     * 密码
     */
    @TableField("password")
    @Schema(description = "密码")
    private String password;

    /**
     * 昵称
     */
    @TableField("nickname")
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 邮箱
     */
    @TableField("email")
    @Schema(description = "邮箱")
    private String email;

    /**
     * 用户头像
     */
    @TableField("avatar_file_id")
    @Schema(description = "用户头像文件ID")
    private Long avatarFileId;

    /**
     * 默认组织ID
     */
    @TableField("default_org_id")
    @Schema(description = "默认组织ID")
    private Long defaultOrgId;

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
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("update_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    @TableField("delete_at")
    @Schema(description = "删除时间")
    @TableLogic
    private LocalDateTime deletedAt;
}