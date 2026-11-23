package online.longlian.app.pojo.bo.orgadmin.presetfield;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationPresetFieldOptionBO {
    private Long id;
    private String name;
}
