package online.longlian.app.pojo.bo.orgadmin;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class OrgMemberExitParamsBO {
    private Long orgId;
    private Long operatorUserId;
}
