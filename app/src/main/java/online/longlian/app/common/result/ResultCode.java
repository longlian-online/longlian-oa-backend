package online.longlian.app.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    USERNAME_OR_PASSWORD_ERROR(1001, "账号或密码错误"),

    SUCCESS(2000, "操作成功"),

    UNAUTHORIZED(4001, "未授权访问"),

    PARAM_ERROR(4002, "参数错误"),

    UNAUTHORIZED_OPERATION(4003,"无权操作"),

    NOT_FOUND(4004, "请求资源不存在"),

    OPERATION_FAIL(4005,"操作失败"),

    DATA_NOT_EXIT(4006,"数据不存在"),

    FAIL(5000, "系统内部异常");
    private final int code;
    private final String msg;
    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}