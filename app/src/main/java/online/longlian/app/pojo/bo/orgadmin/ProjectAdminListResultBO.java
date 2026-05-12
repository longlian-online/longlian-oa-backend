package online.longlian.app.pojo.bo.orgadmin;

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
public class ProjectAdminListResultBO {
    private Long id;
    private String title;
    private Long typeId;
    private String typeName;
    private ProjectStatus status;
    private Long creatorId;
    private String creatorNickname;
    private LocalDateTime createdAt;
}
