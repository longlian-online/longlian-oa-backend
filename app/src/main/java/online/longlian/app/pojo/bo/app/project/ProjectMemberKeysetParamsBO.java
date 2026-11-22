package online.longlian.app.pojo.bo.app.project;

/**
 * 企划成员键集分页入参：与 {@link ProjectMemberListParamsBO} 同口径，按用户 ID 升序，
 * afterUserId 之后取 limit+1 行判定是否还有下一页。
 */
public record ProjectMemberKeysetParamsBO(Long projectId, Long orgId, Long afterUserId, int limit) {
}
