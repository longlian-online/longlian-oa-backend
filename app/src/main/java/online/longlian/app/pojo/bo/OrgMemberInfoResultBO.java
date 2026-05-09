package online.longlian.app.pojo.bo;

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
public class OrgMemberInfoResultBO {
    private Long id;
    private Long userId;
    private String nickname;
    private String username;
    private String avatarUrl;
    private LocalDateTime joinedAt;
    private LocalDateTime lastSubmittedAt;
    private Integer submitCount;
    private String orgRole;
    private Status status;
}
