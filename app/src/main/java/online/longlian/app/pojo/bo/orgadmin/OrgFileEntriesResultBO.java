package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrgFileEntriesResultBO {
    private Long projectId;
    private String projectTitle;
    private Long itemId;
    private String itemTitle;
    private List<OrgFileEntryResultBO> entries;
}
