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
import online.longlian.common.enumeration.OTPType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 邀请码记录表
 * </p>
 *
 * @author longlian
 * @since
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("organization_invite_code")
@ApiModel(value = "OrganizationInviteCode对象", description = "邀请码记录表")
public class OrganizationInviteCode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邀请码记录ID
     */
    @TableId("id")
    @ApiModelProperty("邀请码记录ID")
    private Long id;

    /**
     * 邀请码
     */
    @TableField("code")
    @ApiModelProperty("邀请码")
    private String code;

    /**
     * 业务类型 1-邀请创建组织 2-邀请加入组织
     */
    @TableField("biz_type")
    @ApiModelProperty("业务类型 1-邀请创建组织 2-邀请加入组织")
    private OTPType bizType;

    /**
     * 目标组织ID，创建组织邀请在接受前为空
     */
    @TableField("org_id")
    @ApiModelProperty("目标组织ID，创建组织邀请在接受前为空")
    private Long orgId;

    /**
     * 邀请人ID
     */
    @TableField("creator_id")
    @ApiModelProperty("邀请人ID")
    private Long creatorId;

    /**
     * 被邀请人用户ID，接受后写入
     */
    @TableField("invitee_user_id")
    @ApiModelProperty("被邀请人用户ID，接受后写入")
    private Long inviteeUserId;

    /**
     * 到期时间
     */
    @TableField("expired_at")
    @ApiModelProperty("到期时间")
    private LocalDateTime expiredAt;

    /**
     * 是否接受 0-未接受 1-已接受
     */
    @TableField("accepted")
    @ApiModelProperty("是否接受 0-未接受 1-已接受")
    private Integer accepted;

    /**
     * 接受时间
     */
    @TableField("accepted_at")
    @ApiModelProperty("接受时间")
    private LocalDateTime acceptedAt;

    /**
     * 加入成功后的组织成员ID
     */
    @TableField("org_member_id")
    @ApiModelProperty("加入成功后的组织成员ID")
    private Long orgMemberId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
