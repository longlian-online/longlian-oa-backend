package online.longlian.app.pojo.bo.open;

import java.util.List;

/**
 * 开放接口统一游标分页结果。
 */
public record OpenPageResultBO<T>(List<T> list, String nextCursor, boolean hasMore) {

    public static <T> OpenPageResultBO<T> of(List<T> list, String nextCursor, boolean hasMore) {
        return new OpenPageResultBO<>(list, nextCursor, hasMore);
    }
}
