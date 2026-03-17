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
 * 项目任务节点表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("item_task_node")
@Schema(name = "ItemTaskNode", description = "项目任务节点表")
public class ItemTaskNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 项目任务节点ID
     */
    @TableId("id")
    @Schema(description = "项目任务节点ID")
    private Long id;

    /**
     * 所属项目任务流ID
     */
    @TableField("item_task_flow_id")
    @Schema(description = "所属项目任务流ID")
    private Long itemTaskFlowId;

    /**
     * 所属项目ID
     */
    @TableField("item_id")
    @Schema(description = "所属项目ID")
    private Long itemId;

    /**
     * 所属企划ID
     */
    @TableField("project_id")
    @Schema(description = "所属企划ID")
    private Long projectId;

    /**
     * 关联原子任务ID
     */
    @TableField("base_task_id")
    @Schema(description = "关联原子任务ID")
    private Long baseTaskId;

    /**
     * 任务名称
     */
    @TableField("name")
    @Schema(description = "任务名称")
    private String name;

    /**
     * 步骤顺序
     */
    @TableField("sort")
    @Schema(description = "步骤顺序")
    private Integer sort;

    /**
     * 并行组号排序
     */
    @TableField("parallel_sort")
    @Schema(description = "并行组号排序")
    private Integer parallelSort;

    /**
     * 是否需要上传文件
     */
    @TableField("need_file")
    @Schema(description = "是否需要上传文件 0-否 1-是")
    private Byte needFile;

    /**
     * 是否必须上传
     */
    @TableField("required_file")
    @Schema(description = "是否必须上传 0-否 1-是")
    private Byte requiredFile;

    /**
     * 允许的文件类型
     */
    @TableField("allowed_mime_types")
    @Schema(description = "允许的文件类型（逗号分隔）")
    private String allowedMimeTypes;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}