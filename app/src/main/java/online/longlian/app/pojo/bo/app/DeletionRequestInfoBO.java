package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Data;
import online.longlian.common.enumeration.DeletionTargetType;

import java.time.LocalDateTime;

@Data
@Builder
public class DeletionRequestInfoBO {
    private Long id;
    private DeletionTargetType targetType;
    private Long targetId;
    private Long projectId;
    private String title;
    private String reason;
    private String requesterName;
    private LocalDateTime createdAt;
}
