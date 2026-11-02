package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadResultBO;

import java.util.List;

/**
 * 组员当前接取的任务，按接取时间从早到晚。
 */
@Data
@Builder
public class OrgMemberClaimedTaskResultBO {
    private Long memberId;
    private Long userId;
    private Integer totalCount;
    private Integer submittableCount;
    private Integer waitingCount;
    private List<ClaimedTaskLoadResultBO> list;
}
