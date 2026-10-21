package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.ProjectStatus;

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
    private Long typeId;
    private Long coverFileId;
    private Long thumbnailFileId;
    private ProjectStatus status;
}
