package online.longlian.app.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(2000, "操作成功"),

    FAIL(5000, "系统内部异常"),

    PARAM_ERROR(4000, "参数错误"),

    UNAUTHORIZED(4001, "未授权访问"),

    DATA_NOT_EXIT(4003,"数据不存在"),

    NOT_FOUND(4004, "请求资源不存在"),

    OPERATION_FAIL(4005,"操作失败"),

    UNAUTHORIZED_OPERATION(4006,"无权操作");
    private final int code;
    private final String msg;
    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}