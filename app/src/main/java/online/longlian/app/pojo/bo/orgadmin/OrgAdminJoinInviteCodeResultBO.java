package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgAdminJoinInviteCodeResultBO {
    private Long id;
    private String inviteCode;
    private Integer useCount;
    private Integer maxUses;
    private LocalDateTime expiredAt;
    private String status;
    private LocalDateTime createdAt;
}
