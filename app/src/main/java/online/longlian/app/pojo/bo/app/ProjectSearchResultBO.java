package online.longlian.app.pojo.bo.app;

import lombok.Data;
import online.longlian.app.pojo.entity.Project;

@Data
public class ProjectSearchResultBO {
    private Long projectId;
    private Project project;
    private String matchedMetadataPaths;
}
