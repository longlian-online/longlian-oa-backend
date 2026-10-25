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
@Schema(description = "我的待办条目")
public class MyTaskVO {

    @JsonLongIdString
    @Schema(type = "string", description = "任务实例ID")
    private Long instanceId;

    @JsonLongIdString
    @Schema(type = "string", description = "项目ID")
    private Long itemId;

    @JsonLongIdString
    @Schema(type = "string", description = "企划ID")
    private Long projectId;

    @Schema(description = "项目名")
    private String itemTitle;

    @Schema(description = "阶段名（任务节点名称）")
    private String nodeName;

    @Schema(description = "阶段序号（节点 sort）")
    private Integer stageSort;

    @Schema(description = "本节点在所属阶段并行组中的位次（1 开始，按 parallelSort、节点 ID 排序）")
    private Integer slotIndex;

    @Schema(description = "所属阶段的任务节点数")
    private Integer slotTotal;

    @Schema(description = "同名节点跨阶段时的阶段位次（1 开始）；名称只出现在一个阶段时为 null")
    private Integer serialStep;

    @Schema(description = "企划名")
    private String projectName;

    @Schema(description = "接取时间")
    private LocalDateTime claimedAt;

    @Schema(description = "等待的前置节点名（sort 最大的未完成前置）；仅 WAITING 分组返回，否则为 null")
    private String waitingStageName;

    @Schema(description = "等待的前置节点在其并行组中的位次；非等待时为 null")
    private Integer waitingSlotIndex;

    @Schema(description = "等待的前置节点所属阶段的任务节点数；非等待时为 null")
    private Integer waitingSlotTotal;

    @Schema(description = "等待的前置节点同名跨阶段位次；非等待或名称不跨阶段时为 null")
    private Integer waitingSerialStep;
}
