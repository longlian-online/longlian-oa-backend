package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreateParamsBO {
    private Long orgId;
    private Long creatorId;
    private String title;
    private String alias;
    private Long typeId;
    private String metadata;
    private String description;
    private Long coverFileId;
}
