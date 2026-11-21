package online.longlian.app.common.util;

import online.longlian.app.common.exception.OpenAppException;
import online.longlian.app.common.result.OpenResultCode;
import online.longlian.app.pojo.bo.open.OpenCursorBO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 开放接口查询参数的公共解析：游标与增量时间。各端点复用，保证格式与错误语义一致。
 */
public final class OpenQuerySupport {

    /** 与站内序列化一致的时间格式。 */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    private OpenQuerySupport() {
    }

    /**
     * 解析可选游标；空返回 null，非法按参数错误拒绝。
     */
    public static OpenCursorBO parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        return OpenCursorCodec.decode(cursor);
    }

    /**
     * 解析可选增量起点；空返回 null，非法按参数错误拒绝。
     */
    public static LocalDateTime parseUpdatedSince(String updatedSince) {
        if (updatedSince == null || updatedSince.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(updatedSince, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new OpenAppException(OpenResultCode.PARAM_ERROR, "updatedSince 格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    /**
     * 缺省 limit 取默认值。
     */
    public static int resolveLimit(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : limit;
    }
}
