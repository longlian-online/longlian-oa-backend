package online.longlian.app.pojo.bo.open;

/**
 * 游标解码结果：键集分页的起点。
 * afterUpdatedAtMillis 为 null 表示仅按 afterId 键集分页（如成员按 userId 排序）。
 */
public record OpenCursorBO(Long afterId, Long afterUpdatedAtMillis) {
}
