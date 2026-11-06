package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.TaskDraftStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDraftRecordResultBO {

    private Long id;
    private Long taskInstanceId;
    private Long orgId;
    private Long ownerId;
    private String metadata;
    private Long revision;
    private Long taskRevision;
    private TaskDraftStatus status;
    private Long submittedSubmissionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
