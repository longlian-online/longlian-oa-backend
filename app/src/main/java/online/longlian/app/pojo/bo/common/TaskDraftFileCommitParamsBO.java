package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDraftFileCommitParamsBO {

    private Long userId;
    private Long orgId;
    private Long draftId;
    private String operationToken;
    private Map<Long, Long> fileIdMappings;
}
