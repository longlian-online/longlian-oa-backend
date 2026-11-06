package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDraftConsumeParamsBO {

    private Long draftId;
    private Long expectedRevision;
    private Long submissionId;
    private Long taskRevision;
}
