package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
@ApiModel(value = "User对象", description = "系统用户表")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @ApiModelProperty("用户ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    @TableField("username")
    @ApiModelProperty("用户名")
    private String username;

    /**
     * 密码
     */
    @ApiModelProperty("密码")
    @TableField("password")
    private String password;

    /**
     * 昵称
     */
    @ApiModelProperty("昵称")
    @TableField("nickname")
    private String nickname;

    /**
     * 邮箱
     */
    @TableField("email")
    @ApiModelProperty("邮箱")
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
    @TableLogic
    private LocalDateTime deleteAt;
}
