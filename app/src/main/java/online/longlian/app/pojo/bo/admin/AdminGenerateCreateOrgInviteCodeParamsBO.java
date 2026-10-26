package online.longlian.app.pojo.bo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminGenerateCreateOrgInviteCodeParamsBO {
    private Long creatorId;
    private LocalDateTime expiredAt;
}
