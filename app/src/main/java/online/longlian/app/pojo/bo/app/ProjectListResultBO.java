package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.ProjectStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectListResultBO {
    private Long id;
    private String title;
    private String alias;
    private String description;
    private String coverUrl;
    private String projectType;
    private ProjectStatus projectStatus;
    private String metadata;
    private String creatorAvatarUrl;
    private String creatorUsername;
    private LocalDateTime creatorLastSubmittedAt;
}
