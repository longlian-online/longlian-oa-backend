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
public class OrgAdminGenerateJoinOrgInviteCodeResultBO {
    private Long id;
    private String inviteCode;
    private Integer maxUses;
    private LocalDateTime expiredAt;
}
