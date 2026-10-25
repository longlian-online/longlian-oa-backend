package online.longlian.app.pojo.vo.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.JSONWriter;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@AllArgsConstructor
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
public class NotificationSourceVO {
    @JsonLongIdString
    @Schema(type = "string", description = "企划ID")
    private Long projectId;
    private String projectTitle;
    @JsonLongIdString
    @Schema(type = "string", description = "项目ID")
    private Long itemId;
    private String itemTitle;
    @Schema(nullable = true, description = "任务来源的当前槽位结构；项目完成通知为 null")
    private NotificationTaskSlotVO taskSlot;
}
