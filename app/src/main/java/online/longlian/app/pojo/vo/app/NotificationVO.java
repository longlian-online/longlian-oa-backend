package online.longlian.app.pojo.vo.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import online.longlian.app.common.annotation.JsonLongIdString;
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.JSONWriter;

@Data
@AllArgsConstructor
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
public class NotificationVO {
    @JsonLongIdString
    @Schema(type = "string")
    private Long id;
    @Schema(allowableValues = {"TASK_READY", "ITEM_COMPLETED"})
    private String kind;
    @JsonLongIdString
    @Schema(type = "string")
    private Long sourceId;
    @Schema(type = "string", example = "2026-10-22 12:00:00")
    private LocalDateTime createdAt;
    @Schema(type = "string", example = "2026-10-22 12:00:00", nullable = true)
    private LocalDateTime readAt;
    @Schema(nullable = true, description = "已授权来源的当前基础数据；来源不可访问时为 null")
    private NotificationSourceVO source;
    @Schema(nullable = true, description = "当前页已授权图片读取链接，无可读图片时为 null")
    private String thumbnailUrl;
}
