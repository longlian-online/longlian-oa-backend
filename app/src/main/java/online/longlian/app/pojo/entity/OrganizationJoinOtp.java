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

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 邀请加入组织表
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("organization_join_otp")
@ApiModel(value = "OrganizationJoinOtp对象", description = "邀请加入组织表")
public class OrganizationJoinOtp implements Serializable {

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
     * 目标组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("目标组织ID")
    private Long orgId;

    /**
     * 被邀请用户ID
     */
    @ApiModelProperty("被邀请用户ID")
    @TableField("invited_user_id")
    private Long invitedUserId;

    /**
     * 加入成功后的组织成员ID
     */
    @TableField("org_member_id")
    @ApiModelProperty("加入成功后的组织成员ID")
    private Long orgMemberId;

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
