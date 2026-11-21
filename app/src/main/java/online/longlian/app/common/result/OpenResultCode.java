package online.longlian.app.common.result;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 开放接口（/open/**）专用错误段 10xxx 与真实 HTTP 状态码映射。
 * 站内业务码不出现在开放面；站内异常码经 of 映射为最近的开放语义。
 */
@Getter
public enum OpenResultCode {
    UNAUTHENTICATED(10001, HttpStatus.UNAUTHORIZED, "OPEN_UNAUTHENTICATED", "密钥无效或已吊销"),
    PARAM_ERROR(10002, HttpStatus.BAD_REQUEST, "OPEN_PARAM_ERROR", "参数错误"),
    FORBIDDEN(10003, HttpStatus.FORBIDDEN, "OPEN_FORBIDDEN", "无权访问"),
    NOT_FOUND(10004, HttpStatus.NOT_FOUND, "OPEN_NOT_FOUND", "请求资源不存在"),
    RATE_LIMITED(10005, HttpStatus.TOO_MANY_REQUESTS, "OPEN_RATE_LIMITED", "请求过于频繁"),
    IDEMPOTENCY_CONFLICT(10006, HttpStatus.CONFLICT, "OPEN_IDEMPOTENCY_CONFLICT", "幂等键冲突"),
    INTERNAL(10999, HttpStatus.INTERNAL_SERVER_ERROR, "OPEN_INTERNAL", "系统内部异常");

    private final int code;
    private final HttpStatus httpStatus;
    private final String error;
    private final String msg;

    OpenResultCode(int code, HttpStatus httpStatus, String error, String msg) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.error = error;
        this.msg = msg;
    }

    /**
     * 业务码反查开放语义。未命中的站内码映射到最近语义；OPERATION_FAIL(5) 在开放面
     * 多为状态机/revision 拒绝，按客户端错误处理，其余按内部错误处理。
     */
    public static OpenResultCode of(int code) {
        for (OpenResultCode value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return switch (code) {
            case 2, 5 -> PARAM_ERROR;
            case 3 -> FORBIDDEN;
            case 4, 6 -> NOT_FOUND;
            default -> INTERNAL;
        };
    }
}
