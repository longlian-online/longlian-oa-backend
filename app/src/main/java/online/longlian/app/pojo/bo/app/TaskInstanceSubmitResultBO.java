package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class TaskInstanceSubmitResultBO {
    private Long submissionId;
    private LocalDateTime triggeredAt;
    private List<ItemStageNodeStatusBO> beforeNodes;
}
