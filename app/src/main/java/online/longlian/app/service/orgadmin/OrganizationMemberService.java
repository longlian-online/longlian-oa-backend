package online.longlian.app.service.orgadmin;

import lombok.NonNull;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;

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

