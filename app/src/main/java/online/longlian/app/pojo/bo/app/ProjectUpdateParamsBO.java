package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUpdateParamsBO {
    private Long projectId;
    private Long orgId;
    private Long userId;
    private String title;
    private String alias;
    private String metadata;
    private String description;
    private Long coverFileId;
}
