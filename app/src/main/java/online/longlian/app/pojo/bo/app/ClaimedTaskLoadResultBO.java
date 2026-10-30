package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClaimedTaskLoadResultBO {
    private Long assigneeId;
    private Long instanceId;
    private Long itemId;
    private Long projectId;
    private String itemTitle;
    private String projectName;
    private String nodeName;
    private Integer slotIndex;
    private Integer slotTotal;
    private Integer serialStep;
    private String waitingStageName;
    private Integer waitingSlotIndex;
    private Integer waitingSlotTotal;
    private Integer waitingSerialStep;
}
