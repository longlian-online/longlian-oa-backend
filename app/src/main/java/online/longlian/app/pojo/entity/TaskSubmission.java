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
 * 任务提交记录表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("task_submission")
@Schema(name = "TaskSubmission", description = "任务提交记录表")
public class TaskSubmission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    @Schema(description = "任务提交记录ID")
    private Long id;

    /**
     * 所属企划ID
     */
    @TableField("project_id")
    @Schema(description = "所属企划ID")
    private Long projectId;

    /**
     * 所属项目ID
     */
    @TableField("item_id")
    @Schema(description = "所属项目ID")
    private Long itemId;

    /**
     * 所属任务实例ID
     */
    @TableField("task_instance_id")
    @Schema(description = "所属任务实例ID")
    private Long taskInstanceId;

    /**
     * 关联原子任务ID
     */
    @TableField("base_task_id")
    @Schema(description = "关联原子任务ID")
    private Long baseTaskId;

    /**
     * 提交人ID
     */
    @TableField("submitter_id")
    @Schema(description = "提交人ID")
    private Long submitterId;

    /**
     * 上传文件ID
     */
    @TableField("file_id")
    @Schema(description = "上传文件ID")
    private Long fileId;

    /**
     * 元数据
     */
    @TableField("metadata")
    @Schema(description = "元数据")
    private String metadata;

    /**
     * 提交状态 1-SUBMITTED(已提交) 2-REJECTED(已打回) 3-RESET(已重置)
     */
    @TableField("status")
    @Schema(description = "提交状态 1-SUBMITTED(已提交) 2-REJECTED(已打回) 3-RESET(已重置)")
    private Byte status;

    /**
     * 审核人ID（打回操作人）
     */
    @TableField("reviewer_id")
    @Schema(description = "审核人ID（打回操作人）")
    private Long reviewerId;

    /**
     * 审核/打回时间
     */
    @TableField("reviewed_at")
    @Schema(description = "审核/打回时间")
    private LocalDateTime reviewedAt;

    /**
     * 审核/打回意见
     */
    @TableField("review_comment")
    @Schema(description = "审核/打回意见")
    private String reviewComment;

    @TableField("created_at")
    @Schema(description = "提交时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}