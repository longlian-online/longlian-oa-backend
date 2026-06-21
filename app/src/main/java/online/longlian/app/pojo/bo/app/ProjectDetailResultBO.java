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
public class ProjectDetailResultBO {
    private Long id;
    private String title;
    private String alias;
    private String coverUrl;
    private String typeName;
    private String metadata;
    private String description;
    private ProjectStatus status;
    private Integer progressPercent;
    private Integer claimedTaskCount;
    private Integer pendingTaskCount;
    private Boolean inWorkshop;
    private Boolean isCreator;
}
