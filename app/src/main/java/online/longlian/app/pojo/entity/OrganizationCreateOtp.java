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
 * 邀请创建组织表
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("organization_create_otp")
@ApiModel(value = "OrganizationCreateOtp对象", description = "邀请创建组织表")
public class OrganizationCreateOtp implements Serializable {

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

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
