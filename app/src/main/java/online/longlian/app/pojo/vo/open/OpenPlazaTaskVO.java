package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开放面任务广场任务卡片。字段冻结：只增不删；不含图标。
 */
@Data
@Schema(description = "开放接口缺人任务")
public class OpenPlazaTaskVO {

    @Schema(description = "任务实例ID")
    private Long instanceId;

    @Schema(description = "所属项目ID")
    private Long itemId;

    @Schema(description = "所属项目标题")
    private String itemTitle;

    @Schema(description = "任务节点名")
    private String nodeName;

    @Schema(description = "阶段位次，从 1 开始")
    private Integer stageIndex;

    @Schema(description = "阶段总数")
    private Integer stageTotal;

    @Schema(description = "并行槽位总数")
    private Integer parallelTotal;

    @Schema(description = "已占用槽位数")
    private Integer filledSlots;

    @Schema(description = "缺人槽位数")
    private Integer pendingSlots;

    @Schema(description = "本任务在并行组内的槽位序号，从 1 开始")
    private Integer slotIndex;

    @Schema(description = "本任务所在并行组编号")
    private Integer slotTotal;

    @Schema(description = "同名任务跨阶段出现的位次，仅一个阶段时为空")
    private Integer serialStep;

    @Schema(description = "任务生成时间")
    private LocalDateTime createdAt;

    @Schema(description = "是否条目当前推进阶段")
    private Boolean isCurrentStage;
}
