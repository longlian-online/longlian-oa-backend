package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterByInviteParamsBO {
    private String inviteCode;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String code;
    private String orgName;
}
