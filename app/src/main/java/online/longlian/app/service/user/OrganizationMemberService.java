package online.longlian.app.service.user;

import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.vo.app.InviteInfoVO;
import online.longlian.app.pojo.vo.admin.InviteLinkVO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;

public interface OrganizationMemberService {

    Result<InviteLinkVO> generateInviteLink();

    Result<InviteCodeVO> generateInviteCode();

    Result<InviteInfoVO> getInviteOrgInfo(String inviteToken);

}
