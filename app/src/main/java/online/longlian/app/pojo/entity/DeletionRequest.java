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
import online.longlian.common.enumeration.DeletionRequestStatus;
import online.longlian.common.enumeration.DeletionTargetType;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("deletion_request")
@ApiModel(value = "DeletionRequest对象", description = "删除申请表")
public class DeletionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    @TableField("org_id")
    @ApiModelProperty("所属组织ID")
    private Long orgId;

    @TableField("target_type")
    @ApiModelProperty("目标类型 1-企划 2-项目")
    private DeletionTargetType targetType;

    @TableField("target_id")
    @ApiModelProperty("目标ID")
    private Long targetId;

    @TableField("project_id")
    @ApiModelProperty("所属企划ID")
    private Long projectId;

    @TableField("target_title")
    @ApiModelProperty("提交时的名称")
    private String targetTitle;

    @TableField("reason")
    @ApiModelProperty("删除原因")
    private String reason;

    @TableField("requester_id")
    @ApiModelProperty("申请人ID")
    private Long requesterId;

    @TableField("status")
    @ApiModelProperty("状态 0-待审核 1-通过 2-拒绝")
    private DeletionRequestStatus status;

    @TableField("reviewer_id")
    @ApiModelProperty("审核人ID")
    private Long reviewerId;

    @TableField("reviewed_at")
    @ApiModelProperty("审核时间")
    private LocalDateTime reviewedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
