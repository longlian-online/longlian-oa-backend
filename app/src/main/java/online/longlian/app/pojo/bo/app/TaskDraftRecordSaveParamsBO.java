package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDraftRecordSaveParamsBO {

    private Long instanceId;
    private Long userId;
    private Long orgId;
    private String expectedTaskRevision;
    private String expectedDraftRevision;
    private String metadata;
}
