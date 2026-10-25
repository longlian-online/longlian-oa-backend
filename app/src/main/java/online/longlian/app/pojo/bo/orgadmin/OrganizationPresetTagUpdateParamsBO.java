package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationPresetTagUpdateParamsBO {
    private Long tagId;
    private Long orgId;
    private String name;
    private String color;
    private Integer priority;
}
