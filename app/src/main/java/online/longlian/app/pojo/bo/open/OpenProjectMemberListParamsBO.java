package online.longlian.app.pojo.bo.open;

/**
 * 开放面企划成员键集列表入参。组织上下文来自密钥，不接受调用方指定。
 */
public record OpenProjectMemberListParamsBO(Long projectId, Long orgId, OpenCursorBO cursor, int limit) {
}
