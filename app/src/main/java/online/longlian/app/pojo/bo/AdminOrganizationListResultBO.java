package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import online.longlian.generator.enumeration.Status;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class AdminOrganizationListResultBO {
    private Long id;
    private String name;
    private String avatarUrl;
    private Status status;
    private LocalDateTime createdAt;
}
