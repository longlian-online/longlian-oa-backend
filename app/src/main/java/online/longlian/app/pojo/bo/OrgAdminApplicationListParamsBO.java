package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.SortDirection;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgAdminApplicationListParamsBO {
    private Long orgId;
    private String keyword;
    private LocalDateTime startApplyTime;
    private LocalDateTime endApplyTime;
    private SortDirection orderDir;
    private PageParamsBO page;
}
