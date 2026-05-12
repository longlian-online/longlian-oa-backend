package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.Status;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateDetailResultBO {
    private Long id;
    private String name;
    private String description;
    private Status status;
    private Integer refCount;
    private Long creatorId;
    private String creatorNickname;
    private LocalDateTime createdAt;
    private List<TaskTemplateNodeResultBO> nodes;
}
