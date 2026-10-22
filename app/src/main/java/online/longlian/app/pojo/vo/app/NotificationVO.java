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
    private String kind;
    @JsonLongIdString
    @Schema(type = "string")
    private Long sourceId;
    @Schema(type = "string", example = "2026-10-22 12:00:00")
    private LocalDateTime createdAt;
    @Schema(type = "string", example = "2026-10-22 12:00:00")
    private LocalDateTime readAt;
    private NotificationContentVO content;
}
