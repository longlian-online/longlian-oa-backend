package online.longlian.app.service.user;

import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.vo.app.InviteInfoVO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;

public interface OrganizationMemberService {

    /**
     * 生成管理员邀请码。
     */
    Result<InviteCodeVO> generateInviteCode();

    Result<InviteInfoVO> getInviteOrgInfo(String inviteCode);

}
