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
public class OrgMemberListParamsBO {
    private Long orgId;
    private String keyword;
    private LocalDateTime startJoinedTime;
    private LocalDateTime endJoinedTime;
    private SortDirection orderDir;
    private PageParamsBO page;
}
