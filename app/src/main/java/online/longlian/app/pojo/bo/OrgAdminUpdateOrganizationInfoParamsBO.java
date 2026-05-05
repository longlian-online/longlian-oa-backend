package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgAdminUpdateOrganizationInfoParamsBO {
    private Long orgId;
    private String name;
    private Long avatarFileId;
    private String description;
}
