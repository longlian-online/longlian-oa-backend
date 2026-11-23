package online.longlian.app.pojo.bo.app.item;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企划下项目键集分页结果：lastId/lastUpdatedAt 为本页最后一个项目的键，调用方据此编码下一页游标。
 */
public record ItemKeysetListResultBO(List<ProjectItemListResultBO> list,
                                     Long lastId, LocalDateTime lastUpdatedAt, boolean hasMore) {
}
