package online.longlian.app.pojo.bo.orgadmin.presetfield;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationPresetFieldDeleteParamsBO {
    private Long fieldId;
    private Long orgId;
}
