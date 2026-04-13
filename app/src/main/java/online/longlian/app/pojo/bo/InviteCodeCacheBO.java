package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.InviteMode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteCodeCacheBO {

    /**
     * 邀请码类型
     */
    private InviteMode inviteMode;

    /**
     * 组织ID，创建组织场景为空
     */
    private Long orgId;

    /**
     * 组织名称，创建组织场景为空
     */
    private String orgName;

    /**
     * 过期时间
     */
    private String expireAt;
}
