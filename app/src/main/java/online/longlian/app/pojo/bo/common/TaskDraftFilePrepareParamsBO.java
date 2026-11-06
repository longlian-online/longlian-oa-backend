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
public class TaskDraftFilePrepareParamsBO {

    private Long userId;
    private Long orgId;
    private Long draftId;
    private List<Long> fileIds;
}
