package online.longlian.app.pojo.bo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInviteCodeAcceptParamsBO {
    private Long id;
    private Long inviteeUserId;
    private Long orgId;
    private Long orgMemberId;
}
