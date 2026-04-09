package online.longlian.app.service.user;

import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.JoinByInviteCodeDTO;
import online.longlian.app.pojo.vo.InviteCodeVO;
import online.longlian.app.pojo.vo.InviteInfoVO;
import online.longlian.app.pojo.vo.InviteLinkVO;

public interface OrganizationMemberService {

    Result<InviteLinkVO> generateInviteLink();


    Result<InviteInfoVO> getInviteOrgInfo(String inviteToken);

}
