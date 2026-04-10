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

    FAIL(8, "系统内部异常");
    private final int code;
    private final String msg;
    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}