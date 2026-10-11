package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Data;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;

import java.util.List;

@Data
@Builder
public class ProjectSearchParamsBO {
    private Long orgId;
    private Long typeId;
    private String keyword;
    private String likePattern;
    private List<String> metadataPatterns;
    private SortByTime sortByTime;
    private SortDirection orderDir;
}
