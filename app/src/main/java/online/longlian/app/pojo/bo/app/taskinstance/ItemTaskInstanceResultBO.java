package online.longlian.app.pojo.bo.app.taskinstance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.TaskInstanceStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemTaskInstanceResultBO {

    private Long id;

    private String name;

    private TaskInstanceStatus status;

    private String revision;

    private Long assigneeId;

    private String assigneeNickname;

    private String assigneeAvatarUrl;

    private Integer sort;

    private Integer parallelSort;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
