package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgFileTypeItemBO {
    private Long itemId;
    private String itemTitle;
    private Long projectId;
    private String projectTitle;
}
