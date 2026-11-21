package online.longlian.app.pojo.bo.app.project;

import java.util.List;

/**
 * 企划成员键集分页结果：lastUserId 为本页最后一名成员的用户 ID，调用方据此编码下一页游标。
 */
public record ProjectMemberKeysetResultBO(List<ProjectMemberKeysetItemBO> list, Long lastUserId, boolean hasMore) {

    public static ProjectMemberKeysetResultBO of(List<ProjectMemberKeysetItemBO> list,
                                                 Long lastUserId, boolean hasMore) {
        return new ProjectMemberKeysetResultBO(list, lastUserId, hasMore);
    }
}
