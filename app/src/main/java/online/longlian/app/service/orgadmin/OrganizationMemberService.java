package online.longlian.app.service.orgadmin;

import lombok.NonNull;
import online.longlian.app.pojo.bo.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.bo.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;

public interface OrganizationMemberService {

    /**
     * 分页查询待审核入组申请。
     */
    PageResultBO<OrgAdminApplicationInfoResultBO> listApplications(@NonNull OrgAdminApplicationListParamsBO params);

    /**
     * 审核入组申请。
     */
    void reviewApplication(@NonNull OrgAdminReviewApplicationParamsBO params);

    /**
     * 生成组织加入邀请码。
     */
    OrgAdminGenerateJoinOrgInviteCodeResultBO generateJoinOrgInviteCode(@NonNull OrgAdminGenerateJoinOrgInviteCodeParamsBO params);
}

