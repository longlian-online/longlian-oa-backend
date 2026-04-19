package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.generator.enumeration.Status;

@Data
@AllArgsConstructor
public class AdminOrganizationUpdateStatusParamsBO {
    private Long organizationId;
    private Status status;
}
