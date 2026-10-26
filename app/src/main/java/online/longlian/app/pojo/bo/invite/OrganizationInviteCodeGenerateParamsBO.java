package online.longlian.app.pojo.bo.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.OTPType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInviteCodeGenerateParamsBO {
    private OTPType bizType;
    private Long creatorId;
    private Long orgId;
    private LocalDateTime expiredAt;
}
