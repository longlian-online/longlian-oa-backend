package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGetMyInfoResultBO {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Long defaultOrgId;
}
