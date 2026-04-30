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
import online.longlian.generator.enumeration.ApplicationStatus;
import online.longlian.generator.enumeration.ApplicationType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 入组申请表
 * </p>
 *
 * @author longlian
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("group_application")
@ApiModel(value = "GroupApplication对象", description = "入组申请表")
public class GroupApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    /**
     * 目标组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("目标组织ID")
    private Long orgId;

    /**
     * 申请人ID
     */
    @TableField("user_id")
    @ApiModelProperty("申请人ID")
    private Long userId;

    /**
     * 状态：0-待审核 1-通过 2-拒绝
     */
    @TableField("status")
    @ApiModelProperty("状态：0-待审核 1-通过 2-拒绝")
    private ApplicationStatus status;

    /**
     * 审核人ID
     */
    @ApiModelProperty("审核人ID")
    @TableField("reviewer_id")
    private Long reviewerId;

    /**
     * 审核时间
     */
    @ApiModelProperty("审核时间")
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * 审核备注
     */
    @ApiModelProperty("审核备注")
    @TableField("review_remark")
    private String reviewRemark;

    /**
     * 申请入组的类型：0-注册入组 1-已注册用户入组
     */
    @TableField("application_type")
    @ApiModelProperty("申请入组的类型：0-注册入组 1-已注册用户入组")
    private ApplicationType applicationType;

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

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
