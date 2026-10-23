package online.longlian.app.pojo.bo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.common.enumeration.Status;

@Data
@AllArgsConstructor
public class AdminOrganizationIdentityBO {
    private Long id;
    private String name;
    private Status status;
}
