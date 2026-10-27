package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrgAdminJoinInviteCodeResultBO {
    private Long id;
    private String inviteCode;
    private Integer useCount;
    private Integer maxUses;
    private String status;
    private LocalDateTime createdAt;
}
