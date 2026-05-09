package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.Status;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginSessionCacheBO {
    private Long userId;
    private String username;
    private String email;
    private Status status;
    private Long currentOrgId;
    private List<String> roles;
    private List<String> permissions;
}
