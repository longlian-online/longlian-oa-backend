package online.longlian.app.pojo.bo.orgadmin.projecttype;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTypeDeleteParamsBO {
    private Long typeId;
    private Long orgId;
}
