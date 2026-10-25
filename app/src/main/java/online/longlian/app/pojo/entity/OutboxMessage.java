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
import online.longlian.common.enumeration.OutboxMessageStatus;
import online.longlian.common.enumeration.OutboxMessageType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 通用 outbox
 * </p>
 *
 * @author longlian
 * @since
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("outbox_message")
@ApiModel(value = "OutboxMessage对象", description = "通用 outbox。一种 type 对应一个投递器")
public class OutboxMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId("id")
    @ApiModelProperty("消息ID")
    private Long id;

    /**
     * 投递类型 1-INBOX_EMAIL
     */
    @TableField("type")
    @ApiModelProperty("投递类型 1-INBOX_EMAIL")
    private OutboxMessageType type;

    /**
     * 同一类型内的幂等键
     */
    @TableField("dedupe_key")
    @ApiModelProperty("同一类型内的幂等键")
    private String dedupeKey;

    /**
     * 该 type 专用的 JSON
     */
    @TableField("payload")
    @ApiModelProperty("该 type 专用的 JSON")
    private String payload;

    /**
     * 1-PENDING 2-PROCESSING 3-SENT 4-FAILED 5-SKIPPED
     */
    @TableField("status")
    @ApiModelProperty("1-PENDING 2-PROCESSING 3-SENT 4-FAILED 5-SKIPPED")
    private OutboxMessageStatus status;

    /**
     * 已领取次数，含崩溃后再次领取
     */
    @TableField("attempt_count")
    @ApiModelProperty("已领取次数，含崩溃后再次领取")
    private Integer attemptCount;

    /**
     * PENDING 可领取时间
     */
    @TableField("available_at")
    @ApiModelProperty("PENDING 可领取时间")
    private LocalDateTime availableAt;

    /**
     * PROCESSING 租约
     */
    @TableField("locked_until")
    @ApiModelProperty("PROCESSING 租约")
    private LocalDateTime lockedUntil;

    /**
     * 最近一次失败或跳过原因，不含邮箱地址和正文
     */
    @TableField("last_error")
    @ApiModelProperty("最近一次失败或跳过原因，不含邮箱地址和正文")
    private String lastError;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
