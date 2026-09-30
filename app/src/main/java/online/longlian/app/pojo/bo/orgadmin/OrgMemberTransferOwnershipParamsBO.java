package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgMemberTransferOwnershipParamsBO {
    private Long orgId;
    private Long operatorUserId;
    private Long memberId;
}
