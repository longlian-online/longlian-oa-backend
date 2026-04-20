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
import online.longlian.generator.enumeration.OPTStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 邀请创建组织表
 * </p>
 *
 * @author longlian
 * @since 2026-04-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("organization_create_opt")
@ApiModel(value = "OrganizationCreateOpt对象", description = "邀请创建组织表")
public class OrganizationCreateOpt implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邀请ID
     */
    @TableId("id")
    @ApiModelProperty("邀请ID")
    private Long id;

    /**
     * 关联验证码ID
     */
    @TableField("otp_id")
    @ApiModelProperty("关联验证码ID")
    private Long otpId;

    /**
     * 被邀请用户ID
     */
    @ApiModelProperty("被邀请用户ID")
    @TableField("invited_user_id")
    private Long invitedUserId;

    /**
     * 创建成功后的组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("创建成功后的组织ID")
    private Long orgId;

    /**
     * 状态 0-待使用 1-已使用 2-已过期
     */
    @TableField("status")
    @ApiModelProperty("状态 0-待使用 1-已使用 2-已过期")
    private OPTStatus status;

    /**
     * 使用时间
     */
    @TableField("used_at")
    @ApiModelProperty("使用时间")
    private LocalDateTime usedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
