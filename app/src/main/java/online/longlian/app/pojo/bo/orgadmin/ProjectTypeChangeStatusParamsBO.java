package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.generator.enumeration.Status;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTypeChangeStatusParamsBO {
    private Long typeId;
    private Long orgId;
    private Status status;
}
