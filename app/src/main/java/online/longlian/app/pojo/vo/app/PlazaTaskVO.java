package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "缺人任务（任务广场）")
public class PlazaTaskVO {

    @JsonLongIdString
    @Schema(type = "string", description = "任务实例ID")
    private Long instanceId;

    @JsonLongIdString
    @Schema(type = "string", description = "所属条目ID")
    private Long itemId;

    @Schema(description = "所属条目标题")
    private String itemTitle;

    @Schema(description = "任务名称（节点名）")
    private String nodeName;

    @Schema(description = "任务所属阶段序号（1 开始）")
    private Integer stageIndex;

    @Schema(description = "任务流总阶段数")
    private Integer stageTotal;

    @Schema(description = "任务所属阶段并行槽总数")
    private Integer parallelTotal;

    @Schema(description = "已被占用的并行槽数（已接取或已完成）")
    private Integer filledSlots;

    @Schema(description = "待接取并行槽数")
    private Integer pendingSlots;

    @Schema(description = "本节点在所属阶段并行组中的位次（1 开始，按 parallelSort、节点 ID 排序）")
    private Integer slotIndex;

    @Schema(description = "所属阶段的任务节点数")
    private Integer slotTotal;

    @Schema(description = "同名节点跨阶段时的阶段位次（1 开始）；名称只出现在一个阶段时为 null")
    private Integer serialStep;
    @Schema(description = "任务创建时间（等待起始时间）")
    private LocalDateTime createdAt;

    @Schema(description = "是否属于条目首个未完成阶段")
    private Boolean isCurrentStage;
}
