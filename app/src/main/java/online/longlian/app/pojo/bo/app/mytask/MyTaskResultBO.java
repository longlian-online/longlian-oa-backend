package online.longlian.app.pojo.bo.app.mytask;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyTaskResultBO {

    private Long instanceId;

    private Long itemId;

    private Long projectId;

    private String itemTitle;

    private String nodeName;

    private Integer stageSort;

    private Integer slotIndex;

    private Integer slotTotal;

    private Integer serialStep;

    private String projectName;

    private LocalDateTime claimedAt;

    private String waitingStageName;

    private Integer waitingSlotIndex;

    private Integer waitingSlotTotal;

    private Integer waitingSerialStep;
}
