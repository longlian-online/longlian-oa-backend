package online.longlian.app.pojo.bo.app.mytask;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 我的待办键集分页结果：lastClaimedAt/lastInstanceId 为本页最后一个任务的键，调用方据此编码下一页游标。
 */
public record MyTaskKeysetResultBO(List<MyTaskKeysetItemBO> list,
                                   LocalDateTime lastClaimedAt, Long lastInstanceId, boolean hasMore) {

    public static MyTaskKeysetResultBO of(List<MyTaskKeysetItemBO> list,
                                         LocalDateTime lastClaimedAt, Long lastInstanceId, boolean hasMore) {
        return new MyTaskKeysetResultBO(list, lastClaimedAt, lastInstanceId, hasMore);
    }
}
