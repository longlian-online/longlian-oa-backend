package online.longlian.app.pojo.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class AdminOrganizationListBO extends PageBO {

    private String OrgName;

    private LocalDateTime startCreateTime;

    private LocalDateTime endCreateTime;
}
