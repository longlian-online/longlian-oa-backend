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
 * 系统用户表
 * </p>
 *
 * @author longlian
 * @since 2026-04-18
 */
@Data
@Builder
@NoArgsConstructor
@TableName("user")
@AllArgsConstructor
@ApiModel(value = "User对象", description = "系统用户表")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId("id")
    @ApiModelProperty("用户ID")
    private Long id;

    /**
     * 用户名（登录用）
     */
    @TableField("username")
    @ApiModelProperty("用户名（登录用）")
    private String username;

    /**
     * 密码（加密存储）
     */
    @TableField("password")
    @ApiModelProperty("密码（加密存储）")
    private String password;

    /**
     * 昵称（展示用）
     */
    @TableField("nickname")
    @ApiModelProperty("昵称（展示用）")
    private String nickname;

    /**
     * 邮箱（登录/通知）
     */
    @TableField("email")
    @ApiModelProperty("邮箱（登录/通知）")
    private String email;

    /**
     * 用户头像
     */
    @ApiModelProperty("用户头像")
    @TableField("avatar_file_id")
    private Long avatarFileId;

    /**
     * 默认组织ID
     */
    @ApiModelProperty("默认组织ID")
    @TableField("default_org_id")
    private Long defaultOrgId;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @ApiModelProperty("状态 1-启用 0-禁用")
    private Status status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
