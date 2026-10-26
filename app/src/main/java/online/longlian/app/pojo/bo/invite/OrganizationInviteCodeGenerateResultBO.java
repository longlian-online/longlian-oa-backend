package online.longlian.app.pojo.bo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInviteCodeGenerateResultBO {
    private Long id;
    private String inviteCode;
    private LocalDateTime expiredAt;
}
