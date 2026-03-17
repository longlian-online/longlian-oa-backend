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
 * 用户操作记录表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("user_operation_log")
@Schema(name = "UserOperationLog", description = "用户操作记录表")
public class UserOperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    @Schema(description = "日志ID")
    private Long id;

    /**
     * 操作人ID
     */
    @TableField("user_id")
    @Schema(description = "操作人ID")
    private Long userId;

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
     * 操作类型 1-TASK_CLAIM(接取任务) 2-TASK_SUBMIT(提交任务) 3-TASK_ABANDON(放弃任务) 4-TASK_REJECT(打回任务) 5-TASK_RESET(重置任务) 6-FILE_DOWNLOAD(下载任务)
     */
    @TableField("operation_type")
    @Schema(description = "操作类型 1-TASK_CLAIM(接取任务) 2-TASK_SUBMIT(提交任务) 3-TASK_ABANDON(放弃任务) 4-TASK_REJECT(打回任务) 5-TASK_RESET(重置任务) 6-FILE_DOWNLOAD(下载任务)")
    private Byte operationType;

    /**
     * 操作请求体
     */
    @TableField("request_body")
    @Schema(description = "操作请求体")
    private String requestBody;

    /**
     * 操作时间
     */
    @TableField("created_at")
    @Schema(description = "操作时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}