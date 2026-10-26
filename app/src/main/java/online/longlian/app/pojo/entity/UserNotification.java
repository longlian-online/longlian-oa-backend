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
import online.longlian.common.enumeration.InboxNotificationKind;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 站内通知表
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_notification")
@ApiModel(value = "UserNotification对象", description = "站内通知表")
public class UserNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    @TableId("id")
    @ApiModelProperty("通知ID")
    private Long id;

    /**
     * 组织作用域
     */
    @TableField("org_id")
    @ApiModelProperty("组织作用域")
    private Long orgId;

    /**
     * 固定收件人ID
     */
    @ApiModelProperty("固定收件人ID")
    @TableField("recipient_user_id")
    private Long recipientUserId;

    /**
     * 类型 1-TASK_READY 2-ITEM_COMPLETED 3-TASK_ASSIGNED
     */
    @TableField("kind")
    @ApiModelProperty("类型 1-TASK_READY 2-ITEM_COMPLETED 3-TASK_ASSIGNED")
    private InboxNotificationKind kind;

    /**
     * 类型定义的来源ID
     */
    @TableField("source_id")
    @ApiModelProperty("类型定义的来源ID")
    private Long sourceId;

    /**
     * 类型定义的一次触发ID
     */
    @TableField("trigger_id")
    @ApiModelProperty("类型定义的一次触发ID")
    private Long triggerId;

    /**
     * 已读时间，NULL为未读
     */
    @TableField("read_at")
    @ApiModelProperty("已读时间，NULL为未读")
    private LocalDateTime readAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
