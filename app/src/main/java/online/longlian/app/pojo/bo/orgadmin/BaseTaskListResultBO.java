package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.Status;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseTaskListResultBO {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private String metaSchema;
    private Integer refCount;
    private Status status;
    private LocalDateTime createdAt;
}
