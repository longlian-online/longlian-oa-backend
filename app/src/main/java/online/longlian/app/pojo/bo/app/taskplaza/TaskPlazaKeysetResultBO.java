package online.longlian.app.pojo.bo.app.taskplaza;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务广场键集分页结果：lastProjectId/lastProjectUpdatedAt 为本页最后一个企划的键，调用方据此编码下一页游标。
 */
public record TaskPlazaKeysetResultBO(List<PlazaProjectGroupResultBO> groups,
                                       Long lastProjectId, LocalDateTime lastProjectUpdatedAt, boolean hasMore) {

    public static TaskPlazaKeysetResultBO of(List<PlazaProjectGroupResultBO> groups,
                                             Long lastProjectId, LocalDateTime lastProjectUpdatedAt,
                                             boolean hasMore) {
        return new TaskPlazaKeysetResultBO(groups, lastProjectId, lastProjectUpdatedAt, hasMore);
    }
}
