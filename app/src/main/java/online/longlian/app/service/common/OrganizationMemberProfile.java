package online.longlian.app.service.common;

/**
 * 当前组织里可被选为任务归属人的启用组员。
 */
public record OrganizationMemberProfile(Long userId, String nickname) {
}
