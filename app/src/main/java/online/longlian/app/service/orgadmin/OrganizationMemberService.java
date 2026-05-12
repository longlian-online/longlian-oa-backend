package online.longlian.app.service.orgadmin;

import lombok.NonNull;
import online.longlian.app.pojo.bo.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.bo.OrgMemberBaseTaskSubmitCountParamsBO;
import online.longlian.app.pojo.bo.OrgMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.bo.OrgMemberChangeStatusParamsBO;
import online.longlian.app.pojo.bo.OrgMemberInfoResultBO;
import online.longlian.app.pojo.bo.OrgMemberListParamsBO;
import online.longlian.app.pojo.bo.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;

/**
 * 组织管理员视角的成员管理接口。
 */
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
     * 分页查询组员列表。
     */
    PageResultBO<OrgMemberInfoResultBO> listMembers(@NonNull OrgMemberListParamsBO params);

    /**
     * 启用/禁用组员。
     */
    void changeMemberStatus(@NonNull OrgMemberChangeStatusParamsBO params);

    /**
     * 查询组员各原子任务提交数。
     */
    OrgMemberBaseTaskSubmitCountResultBO getMemberBaseTaskSubmitCounts(OrgMemberBaseTaskSubmitCountParamsBO params);

    /**
     * 生成组织加入邀请码。
     */
    OrgAdminGenerateJoinOrgInviteCodeResultBO generateJoinOrgInviteCode(@NonNull OrgAdminGenerateJoinOrgInviteCodeParamsBO params);
}

