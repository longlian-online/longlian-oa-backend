package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.InboxLinkType;
import online.longlian.app.common.enumeration.InboxMessageType;

import java.time.LocalDateTime;

@Data
@Schema(description = "站内消息响应")
public class InboxMessageVO {

    @Schema(type = "string", description = "消息ID")
    private Long id;

    @Schema(description = "消息类型")
    private InboxMessageType type;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "链接类型")
    private InboxLinkType linkType;

    @Schema(description = "链接值")
    private String linkValue;

    @Schema(description = "关联业务类型")
    private String relatedType;

    @Schema(description = "关联业务ID")
    private Long relatedId;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "阅读时间")
    private LocalDateTime readAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
