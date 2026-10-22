package online.longlian.app.pojo.vo.app;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
public class NotificationTaskSlotVO {
    private String name;
    private int slotIndex;
    private int slotTotal;
    @Schema(nullable = true)
    private Integer serialStep;
}
