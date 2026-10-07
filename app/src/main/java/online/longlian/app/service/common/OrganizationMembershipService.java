package online.longlian.app.service.common;

import online.longlian.app.pojo.entity.OrganizationMember;

/**
 * 按一次请求声明的组织校验成员关系。
 * <p>
 * 不读写 Redis：请求作用域只来自 {@code X-Org-Id}，登录建议只读 {@code user.default_org_id}。
 */
public interface OrganizationMembershipService {

    /**
     * 要求用户是指定组织的启用成员。组织或成员不可用时直接失败，不回退到其他组织。
     */
    OrganizationMember requireEnabledMember(Long userId, Long orgId);

    /**
     * 为登录挑选建议打开的组织。默认组织不可用时改用最早加入且仍启用的组织。
     * 建议值不写入缓存，也不授权后续请求。
     */
    OrganizationMember suggestForLogin(Long userId);
}
