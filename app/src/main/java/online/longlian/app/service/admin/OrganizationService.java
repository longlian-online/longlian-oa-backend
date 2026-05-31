package online.longlian.app.service.admin;

import lombok.NonNull;
import online.longlian.app.pojo.bo.admin.AdminGenerateCreateOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.admin.AdminGenerateInviteCodeResultBO;
import online.longlian.app.pojo.bo.admin.AdminOrganizationListParamsBO;
import online.longlian.app.pojo.bo.admin.AdminOrganizationListResultBO;
import online.longlian.app.pojo.bo.admin.AdminOrganizationUpdateStatusParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;

/**
 * 系统管理端组织管理服务接口。
 * <p>
 * 提供超级管理员视角的全平台组织管理能力：
 * <ol>
 *   <li><b>组织列表</b>：分页查询所有组织信息，含头像 URL</li>
 *   <li><b>生成邀请码</b>：通过
 *       {@link online.longlian.app.service.otp.OTPServiceFactory OTP 策略工厂} 生成创建组织的一次性邀请码</li>
 *   <li><b>状态管理</b>：启用/禁用组织</li>
 * </ol>
 */
public interface OrganizationService {

    /**
     * 分页查询组织列表（含头像访问 URL）。
     *
     * @param params 包含分页参数的查询参数
     * @return 分页的组织列表
     */
    PageResultBO<AdminOrganizationListResultBO> getOrgListInfo(@NonNull AdminOrganizationListParamsBO params);

    /**
     * 生成创建组织的邀请码。
     *
     * @param params 包含创建者 ID 的生成参数
     * @return 包含邀请码和过期时间的结果
     */
    AdminGenerateInviteCodeResultBO generateCreateOrgInviteCode(@NonNull AdminGenerateCreateOrgInviteCodeParamsBO params);

    /**
     * 更新组织状态（启用/禁用）。
     *
     * @param params 包含组织 ID 和目标状态的变更参数
     */
    void updateOrgStatus(@NonNull AdminOrganizationUpdateStatusParamsBO params);
}
