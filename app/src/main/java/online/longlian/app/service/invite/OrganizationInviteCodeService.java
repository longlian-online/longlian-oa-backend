package online.longlian.app.service.invite;

import online.longlian.app.pojo.bo.invite.OrganizationInviteCodeAcceptParamsBO;
import online.longlian.app.pojo.bo.invite.OrganizationInviteCodeGenerateParamsBO;
import online.longlian.app.pojo.bo.invite.OrganizationInviteCodeGenerateResultBO;
import online.longlian.app.pojo.entity.OrganizationInviteCode;
import online.longlian.common.enumeration.OTPType;

public interface OrganizationInviteCodeService {

    OrganizationInviteCodeGenerateResultBO generate(OrganizationInviteCodeGenerateParamsBO params);

    OrganizationInviteCode getValid(String code, OTPType bizType);

    void accept(OrganizationInviteCodeAcceptParamsBO params);

    /**
     * 审批通过后补被邀请人和成员关系。邀请码在提交申请时已经接受。
     */
    void bindInvitee(Long inviteCodeId, Long inviteeUserId, Long orgMemberId);
}
