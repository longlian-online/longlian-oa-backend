package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadResultBO;
import online.longlian.common.enumeration.Status;

import java.util.List;

@Data
@Builder
public class OrgMemberWorkloadResultBO {
    private Long id;
    private Long userId;
    private String nickname;
    private String username;
    private String avatarUrl;
    private Status status;
    private String note;
    private Integer claimedCount;
    private Integer readyCount;
    private Integer waitingCount;
    private List<ClaimedTaskLoadResultBO> tasks;
}
