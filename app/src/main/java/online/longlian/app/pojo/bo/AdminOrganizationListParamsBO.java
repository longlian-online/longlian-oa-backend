package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminOrganizationListParamsBO  {

    private String OrgName;

    private LocalDateTime startCreateTime;

    private LocalDateTime endCreateTime;

    private PageParamsBO page;
}
