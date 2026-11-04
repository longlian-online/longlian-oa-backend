package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TaskInstanceRejectResultBO {
    private Long submissionId;
    private LocalDateTime triggeredAt;
}
