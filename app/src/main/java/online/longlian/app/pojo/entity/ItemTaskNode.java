package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/**
 * <p>
 * 项目任务节点表
 * </p>
 *
 * @author longlian
 * @since 2026-04-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("item_task_node")
@ApiModel(value = "ItemTaskNode对象", description = "项目任务节点表")
public class ItemTaskNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目任务节点ID
     */
    @TableId("id")
    @ApiModelProperty("项目任务节点ID")
    private Long id;

    /**
     * 所属项目任务流ID
     */
    @ApiModelProperty("所属项目任务流ID")
    @TableField("item_task_flow_id")
    private Long itemTaskFlowId;

    /**
     * 所属项目ID
     */
    @TableField("item_id")
    @ApiModelProperty("所属项目ID")
    private Long itemId;

    /**
     * 所属企划ID
     */
    @TableField("project_id")
    @ApiModelProperty("所属企划ID")
    private Long projectId;

    /**
     * 关联原子任务ID
     */
    @TableField("base_task_id")
    @ApiModelProperty("关联原子任务ID")
    private Long baseTaskId;

    /**
     * 任务名称
     */
    @TableField("name")
    @ApiModelProperty("任务名称")
    private String name;

    /**
     * 步骤顺序
     */
    @TableField("sort")
    @ApiModelProperty("步骤顺序")
    private Integer sort;

    /**
     * 并行组号排序
     */
    @ApiModelProperty("并行组号排序")
    @TableField("parallel_sort")
    private Integer parallelSort;

    /**
     * 是否需要上传文件
     */
    @TableField("need_file")
    @ApiModelProperty("是否需要上传文件")
    private Byte needFile;

    /**
     * 是否必须上传
     */
    @ApiModelProperty("是否必须上传")
    @TableField("required_file")
    private Byte requiredFile;

    /**
     * 允许的文件类型
     */
    @ApiModelProperty("允许的文件类型")
    @TableField("allowed_mime_types")
    private String allowedMimeTypes;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
