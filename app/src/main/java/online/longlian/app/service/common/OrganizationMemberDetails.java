package online.longlian.app.service.common;

/** 当前组织未删除成员的公开档案，不暴露成员状态或组织角色。 */
public record OrganizationMemberDetails(Long userId, String nickname, Long avatarFileId) {
}
