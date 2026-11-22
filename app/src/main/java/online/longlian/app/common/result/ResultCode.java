package online.longlian.app.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(0, "操作成功"),

    UNAUTHORIZED(1, "未授权访问"),

    PARAM_ERROR(2, "参数错误"),

    UNAUTHORIZED_OPERATION(3,"无权操作"),

    NOT_FOUND(4, "请求资源不存在"),

    OPERATION_FAIL(5,"操作失败"),

    DATA_NOT_EXIT(6,"数据不存在"),

    USER_NOT_EXIT(7,"用户不存在"),

    FAIL(8, "系统内部异常"),

    CAPTCHA_REQUIRED(9, "请完成人机验证"),

    CAPTCHA_INVALID(10, "人机验证未通过，请重试"),

    CAPTCHA_UNAVAILABLE(11, "人机验证暂时不可用，请稍后重试"),

    OPEN_RATE_LIMITED(12, "请求过于频繁，请稍后重试"),

    OPEN_IDEMPOTENCY_CONFLICT(13, "幂等键冲突或首笔仍在处理中，请稍后重试");
    private final int code;
    private final String msg;
    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}