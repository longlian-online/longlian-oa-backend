package online.longlian.app.service.common;

import online.longlian.app.pojo.entity.OrganizationMember;

/**
 * 按一次请求声明的组织校验成员关系。
 * <p>
 * 不读写 Redis：请求作用域只来自 {@code X-Org-Id}。登录只读取用户保存的默认组织。
 */
public interface OrganizationMembershipService {

    /**
     * 要求用户是指定组织的启用成员。组织或成员不可用时直接失败，不回退到其他组织。
     */
    OrganizationMember requireEnabledMember(Long userId, Long orgId);

    /**
     * 代提交指定的组员必须是当前组织的启用成员。文案针对被选中的人，而不是操作者。
     */
    OrganizationMember requireAssignableMember(Long userId, Long orgId);

    /**
     * 列出当前组织全部启用组员，供企划负责人选择代提交归属人。
     */
    java.util.List<OrganizationMemberProfile> listEnabledMembers(Long orgId);

    /**
     * 当前组织启用中的组织管理员和组织所有者。审核通知只发给这些人。
     */
    java.util.List<Long> listEnabledAdministratorIds(Long orgId);

    /**
     * 读取用户默认组织。没有默认组织时组织 ID 为空。
     * 默认组织已禁用或用户不是启用成员时仍返回已保存的组织 ID，角色为空，不改用其他组织。
     */
    DefaultOrganization findDefault(Long userId);
}
