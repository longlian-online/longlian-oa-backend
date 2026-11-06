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
public class TaskDraftFileBindParamsBO {

    private Long userId;
    private Long orgId;
    private Long draftId;
    /** 上一份仍有效的草稿。保留的附件从这里改绑到新草稿，不在这里废弃。 */
    private Long previousDraftId;
    private List<Long> fileIds;
}
