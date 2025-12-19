package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户信息表
 * </p>
 *
 * @author longlian
 * @since 2025-12-19
 */
@Getter
@Setter
@ToString
@TableName("user")
@ApiModel(value = "User对象", description = "用户信息表")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（主键）
     */
    @ApiModelProperty("用户ID（主键）")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（唯一）
     */
    @TableField("username")
    @ApiModelProperty("用户名（唯一）")
    private String username;

    /**
     * 加密后的密码（建议用BCrypt/SHA256）
     */
    @TableField("password")
    @ApiModelProperty("加密后的密码（建议用BCrypt/SHA256）")
    private String password;

    /**
     * 用户昵称
     */
    @TableField("nickname")
    @ApiModelProperty("用户昵称")
    private String nickname;

    /**
     * 手机号（可用于登录）
     */
    @TableField("phone")
    @ApiModelProperty("手机号（可用于登录）")
    private String phone;

    /**
     * 邮箱（可用于登录）
     */
    @TableField("email")
    @ApiModelProperty("邮箱（可用于登录）")
    private String email;

    /**
     * 性别：0-未知 1-男 2-女
     */
    @TableField("gender")
    @ApiModelProperty("性别：0-未知 1-男 2-女")
    private Byte gender;

    /**
     * 账号状态：1-正常 2-禁用 3-锁定
     */
    @TableField("status")
    @ApiModelProperty("账号状态：1-正常 2-禁用 3-锁定")
    private Byte status;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 创建人ID（0表示系统）
     */
    @TableField("create_by")
    @ApiModelProperty("创建人ID（0表示系统）")
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField("update_by")
    @ApiModelProperty("更新人ID")
    private Long updateBy;
}
