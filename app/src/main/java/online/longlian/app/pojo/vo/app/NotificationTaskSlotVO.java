package online.longlian.app.pojo.vo.app;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
@Schema(description = "通知关联的任务槽位")
public class NotificationTaskSlotVO {

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "并行组内位次")
    private int slotIndex;

    @Schema(description = "并行组任务数")
    private int slotTotal;

    @Schema(nullable = true, description = "同名节点跨阶段时的阶段位次；名称只出现在一个阶段时为 null")
    private Integer serialStep;
}
