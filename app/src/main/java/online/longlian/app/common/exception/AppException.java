package online.longlian.app.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.common.result.ResultCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppException extends RuntimeException {

    private int code;

    private String msg;

    public AppException() {
        super();
    }

    /**
     * 直接携带消息的构造，供子类把异常文本传给 RuntimeException（日志与 getMessage 可见）。
     */
    protected AppException(String msg) {
        super(msg);
    }
    /**
     * 基于ResultCode枚举构造
     */
    public AppException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
    }
    public AppException(ResultCode resultCode,String msg) {
        super(msg);
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg() +","+ msg;
    }
}