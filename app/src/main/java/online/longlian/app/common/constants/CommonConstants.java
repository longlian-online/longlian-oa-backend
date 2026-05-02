package online.longlian.app.common.constants;

public class CommonConstants {
    public static final String RESPONSE_HEADER_TRACE_ID = "X-Trace-Id";
    public static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    public static final Long CODE_EXPIRE = 300L;
    public static final Long CODE_LIMIT = 60L;
    public static final Integer CODE_LENGTH = 6;
    public static final long FILE_UPLOAD_MAX_SIZE = 50L * 1024 * 1024;

    public static final String NOTIFY_TITLE = "这个是通知标题";

    public static final String THREAD_NAME_PREFIX = "verify-code-";
}
