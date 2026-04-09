package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteCacheBO {

    private String inviteMode;

    private Long orgId;

    private String orgName;

    private String expireAt;
}
