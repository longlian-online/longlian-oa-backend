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

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 入组申请表
 * </p>
 *
 * @author longlian
 * @since 2026-04-18
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

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
