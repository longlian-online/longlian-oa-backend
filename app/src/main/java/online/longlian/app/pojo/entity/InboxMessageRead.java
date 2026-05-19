package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@TableName("inbox_message_read")
@AllArgsConstructor
@Schema(description = "站内消息阅读记录表")
public class InboxMessageRead implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    @Schema(description = "主键ID")
    private Long id;

    @TableField("message_id")
    @Schema(description = "消息ID")
    private Long messageId;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("read_at")
    @Schema(description = "阅读时间")
    private LocalDateTime readAt;
}
