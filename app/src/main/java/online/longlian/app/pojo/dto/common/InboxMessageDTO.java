package online.longlian.app.pojo.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.InboxLinkType;
import online.longlian.app.common.enumeration.InboxMessageType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "站内消息发送参数")
public class InboxMessageDTO {

    @Schema(description = "消息类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private InboxMessageType type;

    @Schema(description = "消息标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "链接类型")
    private InboxLinkType linkType;

    @Schema(description = "链接值（站内相对路径或站外URL）")
    private String linkValue;

    @Schema(description = "关联业务类型（如project/task）")
    private String relatedType;

    @Schema(description = "关联业务ID")
    private Long relatedId;
}
