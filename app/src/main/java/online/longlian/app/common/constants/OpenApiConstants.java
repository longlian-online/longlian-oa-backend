package online.longlian.app.common.constants;

/**
 * 开放接口（/open/**）通用常量。
 */
public final class OpenApiConstants {

    /** 密钥认证头 */
    public static final String API_KEY_HEADER = "X-Api-Key";

    /** 写接口必带的幂等键头，调用方生成（如 UUID） */
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    /** /open/** 路径前缀（带尾斜杠） */
    public static final String OPEN_PATH_PREFIX = "/open/";

    /** 请求体缓存过滤器写入的请求属性：原始请求体的 SHA-256 十六进制 */
    public static final String BODY_HASH_ATTRIBUTE = "openApiBodyHash";

    private OpenApiConstants() {
    }
}
