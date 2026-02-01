package online.longlian.app.common.exception;

import lombok.Data;
import online.longlian.app.common.result.ResultCode;

@Data
public class AppException extends RuntimeException {

    private int code;

    private String msg;

    public AppException() {
        super();
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