package online.longlian.app.pojo.bo.app.taskplaza;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlazaTaskResultBO {

    private Long instanceId;

    private Long itemId;

    private String itemTitle;

    private String nodeName;

    /** 原子任务当前图标（Lucide 组件名），活读不快照；原子任务缺失时为 null */
    private String baseTaskIcon;

    private Integer stageIndex;

    private Integer stageTotal;

    private Integer parallelTotal;

    private Integer filledSlots;

    private Integer pendingSlots;

    private Integer slotIndex;

    private Integer slotTotal;

    private Integer serialStep;

    private LocalDateTime createdAt;

    private Boolean isCurrentStage;
}
