package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDraftFilePromoteParamsBO {

    private Long userId;
    private Long orgId;
    private Long draftId;
    private Long instanceId;
    private List<Long> fileIds;
}
