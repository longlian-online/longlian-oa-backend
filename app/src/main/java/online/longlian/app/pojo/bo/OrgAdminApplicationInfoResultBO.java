package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgAdminApplicationInfoResultBO {
    private Long id;
    private Long userId;
    private String nickname;
    private String username;
    private String avatarUrl;
    private LocalDateTime appliedAt;
}
