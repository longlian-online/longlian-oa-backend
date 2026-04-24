package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginSessionCacheBO {
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private Integer status;
    private Long avatarFileId;
    private Long defaultOrgId;
    private List<String> roles;
    private List<String> permissions;
}
