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
import online.longlian.common.enumeration.TaskDraftStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务最新草稿
 * </p>
 *
 * @author longlian
 * @since
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_draft")
@ApiModel(value = "TaskDraft对象", description = "任务最新草稿")
public class TaskDraft implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 草稿ID
     */
    @TableId("id")
    @ApiModelProperty("草稿ID")
    private Long id;

    /**
     * 所属任务实例ID
     */
    @ApiModelProperty("所属任务实例ID")
    @TableField("task_instance_id")
    private Long taskInstanceId;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("所属组织ID")
    private Long orgId;

    /**
     * 草稿所有者ID（暂存时的任务接取人）
     */
    @TableField("owner_id")
    @ApiModelProperty("草稿所有者ID（暂存时的任务接取人）")
    private Long ownerId;

    /**
     * 草稿元数据(JSON对象，按节点 meta_schema 组织；提交或废弃后清空)
     */
    @TableField("metadata")
    @ApiModelProperty("草稿元数据(JSON对象，按节点 meta_schema 组织；提交或废弃后清空)")
    private String metadata;

    /**
     * 草稿修订号（暂存或废弃时递增，复用记录时不重置）
     */
    @TableField("revision")
    @ApiModelProperty("草稿修订号（暂存或废弃时递增，复用记录时不重置）")
    private Long revision;

    /**
     * 暂存时的任务状态修订号（正式提交时须与任务当前 revision 一致）
     */
    @TableField("task_revision")
    @ApiModelProperty("暂存时的任务状态修订号（正式提交时须与任务当前 revision 一致）")
    private Long taskRevision;

    /**
     * 草稿状态 1-ACTIVE(有效) 2-CONSUMED(已提交) 3-DISCARDED(已废弃)
     */
    @TableField("status")
    @ApiModelProperty("草稿状态 1-ACTIVE(有效) 2-CONSUMED(已提交) 3-DISCARDED(已废弃)")
    private TaskDraftStatus status;

    /**
     * 正式提交记录ID（同一草稿版本重复提交时返回原结果）
     */
    @TableField("submitted_submission_id")
    @ApiModelProperty("正式提交记录ID（同一草稿版本重复提交时返回原结果）")
    private Long submittedSubmissionId;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
