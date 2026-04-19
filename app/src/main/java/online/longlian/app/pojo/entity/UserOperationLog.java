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
import online.longlian.generator.enumeration.UserOperationType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户操作记录表
 * </p>
 *
 * @author longlian
 * @since 2026-04-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_operation_log")
@ApiModel(value = "UserOperationLog对象", description = "用户操作记录表")
public class UserOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    /**
     * 操作人ID
     */
    @TableField("user_id")
    @ApiModelProperty("操作人ID")
    private Long userId;

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
     * 操作类型 1-TASK_CLAIM(接取任务) 2-TASK_SUBMIT(提交任务) 3-TASK_ABANDON(放弃任务) 4-TASK_REJECT(打回任务) 5-TASK_RESET(重置任务) 6-FILE_DOWNLOAD(下载任务)
     */
    @TableField("operation_type")
    @ApiModelProperty("操作类型 1-TASK_CLAIM(接取任务) 2-TASK_SUBMIT(提交任务) 3-TASK_ABANDON(放弃任务) 4-TASK_REJECT(打回任务) 5-TASK_RESET(重置任务) 6-FILE_DOWNLOAD(下载任务)")
    private UserOperationType operationType;

    /**
     * 操作请求体
     */
    @ApiModelProperty("操作请求体")
    @TableField("request_body")
    private String requestBody;

    /**
     * 操作时间
     */
    @ApiModelProperty("操作时间")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
