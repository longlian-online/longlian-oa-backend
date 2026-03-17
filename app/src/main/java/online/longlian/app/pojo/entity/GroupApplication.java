package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 入组申请表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("group_application")
@Schema(name = "GroupApplication", description = "入组申请表")
public class GroupApplication implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    @Schema(description = "入组申请ID")
    private Long id;

    /**
     * 目标组织ID
     */
    @TableField("org_id")
    @Schema(description = "目标组织ID")
    private Long orgId;

    /**
     * 申请人ID
     */
    @TableField("user_id")
    @Schema(description = "申请人ID")
    private Long userId;

    /**
     * 状态：0-待审核 1-通过 2-拒绝
     */
    @TableField("status")
    @Schema(description = "状态：0-待审核 1-通过 2-拒绝")
    private Byte status;

    /**
     * 审核人ID
     */
    @TableField("reviewer_id")
    @Schema(description = "审核人ID")
    private Long reviewerId;

    /**
     * 审核时间
     */
    @TableField("reviewed_at")
    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;

    /**
     * 审核备注
     */
    @TableField("review_remark")
    @Schema(description = "审核备注")
    private String reviewRemark;

    @TableField("created_at")
    @Schema(description = "申请时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}