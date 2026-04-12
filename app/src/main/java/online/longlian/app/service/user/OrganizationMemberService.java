package online.longlian.app.service.user;

import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.vo.app.InviteInfoVO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;

public interface OrganizationMemberService {

    Result<InviteCodeVO> generateRegisterInviteCode();

    Result<InviteCodeVO> generateJoinInviteCode();

    Result<InviteInfoVO> getInviteOrgInfo(String inviteCode);

}
