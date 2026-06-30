package online.longlian.app.pojo.bo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.pojo.bo.common.PageParamsBO;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminOrganizationListParamsBO  {

    private String OrgName;

    private LocalDateTime startCreateTime;

    private LocalDateTime endCreateTime;

    private PageParamsBO page;
}
