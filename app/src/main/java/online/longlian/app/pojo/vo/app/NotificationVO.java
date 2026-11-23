package online.longlian.app.pojo.vo.app;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
@Schema(description = "站内通知")
public class NotificationVO {

    @JsonLongIdString
    @Schema(type = "string", description = "通知ID")
    private Long id;

    @Schema(description = "通知类型",
            allowableValues = {"TASK_READY", "ITEM_COMPLETED", "TASK_ASSIGNED", "JOIN_APPLICATION",
                    "DELETION_REQUEST", "TASK_REJECTED"})
    private String kind;

    @JsonLongIdString
    @Schema(type = "string", description = "来源对象ID")
    private Long sourceId;

    @Schema(type = "string", example = "2026-10-22 12:00:00", description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(type = "string", example = "2026-10-22 12:00:00", nullable = true, description = "已读时间，未读时为 null")
    private LocalDateTime readAt;

    @Schema(nullable = true, description = "已授权来源的当前基础数据；来源不可访问时为 null")
    private NotificationSourceVO source;

    @Schema(nullable = true, description = "当前页已授权图片读取链接，无可读图片时为 null")
    private String thumbnailUrl;
}
