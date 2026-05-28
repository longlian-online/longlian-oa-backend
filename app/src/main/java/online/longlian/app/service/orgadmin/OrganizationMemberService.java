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
 * 组织管理端成员管理服务接口。
 * <p>
 * 提供组织管理员视角的成员管理能力：
 * <ol>
 *   <li><b>加入申请审核</b>：分页查看待审核申请，通过/拒绝并生成正式组织成员关系</li>
 *   <li><b>成员管理</b>：分页查询成员列表（支持搜索/筛选），启用/禁用成员状态</li>
 *   <li><b>任务统计</b>：查看成员在各基础任务上的提交进度</li>
 *   <li><b>邀请码</b>：生成组织加入邀请码，内部通过
 *       {@link online.longlian.app.service.otp.OTPServiceFactory OTP 策略工厂} 管理生命周期</li>
 * </ol>
 */
public interface OrganizationMemberService {

    /**
     * 分页查询待审核的入组申请。
     *
     * @param params 包含分页参数、搜索关键词及组织 ID 的查询参数
     * @return 分页的待审核申请列表
     */
    PageResultBO<OrgAdminApplicationInfoResultBO> listApplications(@NonNull OrgAdminApplicationListParamsBO params);

    /**
     * 审核入组申请（通过/拒绝）。
     * <p>
     * 审核通过时会正式创建组织成员关系。
     *
     * @param params 包含申请 ID、审核结果及审核者信息的参数
     */
    void reviewApplication(@NonNull OrgAdminReviewApplicationParamsBO params);

    /**
     * 分页查询组织成员列表，支持关键词搜索和状态筛选。
     *
     * @param params 包含分页参数、搜索关键词、状态筛选及组织 ID 的查询参数
     * @return 分页的成员信息列表
     */
    PageResultBO<OrgMemberInfoResultBO> listMembers(@NonNull OrgMemberListParamsBO params);

    /**
     * 启用或禁用组织成员。
     *
     * @param params 包含成员 ID、目标状态及组织 ID 的变更参数
     */
    void changeMemberStatus(@NonNull OrgMemberChangeStatusParamsBO params);

    /**
     * 查询指定成员在各基础任务上的提交数量统计。
     *
     * @param params 包含成员 ID 和组织 ID 的查询参数
     * @return 包含各基础任务提交数量的统计结果
     */
    OrgMemberBaseTaskSubmitCountResultBO getMemberBaseTaskSubmitCounts(OrgMemberBaseTaskSubmitCountParamsBO params);

    /**
     * 生成组织加入邀请码。
     *
     * @param params 包含组织 ID 和邀请码有效期的生成参数
     * @return 包含邀请码及相关元信息的结果
     */
    OrgAdminGenerateJoinOrgInviteCodeResultBO generateJoinOrgInviteCode(@NonNull OrgAdminGenerateJoinOrgInviteCodeParamsBO params);
}

