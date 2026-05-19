package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.InboxLinkType;
import online.longlian.app.common.enumeration.InboxMessageType;
import online.longlian.app.common.enumeration.InboxTargetType;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@TableName("inbox_message")
@AllArgsConstructor
@Schema(description = "站内消息表")
public class InboxMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    @Schema(description = "消息ID")
    private Long id;

    @TableField("type")
    @Schema(description = "消息类型")
    private InboxMessageType type;

    @TableField("title")
    @Schema(description = "消息标题")
    private String title;

    @TableField("content")
    @Schema(description = "消息内容")
    private String content;

    @TableField("target_type")
    @Schema(description = "目标类型 1-个人 2-组织")
    private InboxTargetType targetType;

    @TableField("target_id")
    @Schema(description = "目标ID（用户ID或组织ID）")
    private Long targetId;

    @TableField("link_type")
    @Schema(description = "链接类型 1-站内 2-站外")
    private InboxLinkType linkType;

    @TableField("link_value")
    @Schema(description = "链接值")
    private String linkValue;

    @TableField("related_type")
    @Schema(description = "关联业务类型")
    private String relatedType;

    @TableField("related_id")
    @Schema(description = "关联业务ID")
    private Long relatedId;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
