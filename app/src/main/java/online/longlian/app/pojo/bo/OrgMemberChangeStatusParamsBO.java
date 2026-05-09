package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.Status;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgMemberChangeStatusParamsBO {
    private Long orgId;
    private Long memberId;
    private Status status;
}
