package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrgFileEntriesResultBO {
    private Long typeId;
    private String typeName;
    private Long projectId;
    private String projectTitle;
    private Long itemId;
    private String itemTitle;
    private Long taskId;
    private String taskName;
    private Long total;
    private List<OrgFileEntryResultBO> entries;
}
