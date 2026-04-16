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
import online.longlian.app.common.enumeration.TaskSubmissionStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务提交记录表
 * </p>
 *
 * @author longlian
 * @since 2026-04-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_submission")
@ApiModel(value = "TaskSubmission对象", description = "任务提交记录表")
public class TaskSubmission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    /**
     * 所属企划ID
     */
    @TableField("project_id")
    @ApiModelProperty("所属企划ID")
    private Long projectId;

    /**
     * 所属项目ID
     */
    @TableField("item_id")
    @ApiModelProperty("所属项目ID")
    private Long itemId;

    /**
     * 所属任务实例ID
     */
    @ApiModelProperty("所属任务实例ID")
    @TableField("task_instance_id")
    private Long taskInstanceId;

    /**
     * 关联项目任务节点ID
     */
    @ApiModelProperty("关联项目任务节点ID")
    @TableField("item_task_node_id")
    private Long itemTaskNodeId;

    /**
     * 提交人ID
     */
    @ApiModelProperty("提交人ID")
    @TableField("submitter_id")
    private Long submitterId;

    /**
     * 提交元数据(JSON对象，按节点 meta_schema 组织)
     */
    @TableField("metadata")
    @ApiModelProperty("提交元数据(JSON对象，按节点 meta_schema 组织)")
    private String metadata;

    /**
     * 提交状态 1-SUBMITTED(已提交) 2-REJECTED(已打回) 3-RESET(已重置)
     */
    @TableField("status")
    @ApiModelProperty("提交状态 1-SUBMITTED(已提交) 2-REJECTED(已打回) 3-RESET(已重置)")
    private TaskSubmissionStatus status;

    /**
     * 审核人ID（打回操作人）
     */
    @TableField("reviewer_id")
    @ApiModelProperty("审核人ID（打回操作人）")
    private Long reviewerId;

    /**
     * 审核/打回时间
     */
    @TableField("reviewed_at")
    @ApiModelProperty("审核/打回时间")
    private LocalDateTime reviewedAt;

    /**
     * 审核/打回意见
     */
    @ApiModelProperty("审核/打回意见")
    @TableField("review_comment")
    private String reviewComment;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
