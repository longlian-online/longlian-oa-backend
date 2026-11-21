package online.longlian.app.common.exception;

import online.longlian.app.common.result.OpenResultCode;

/**
 * 开放接口业务异常：走全局异常处理器，由开放端响应 advice 转为 10xxx 信封与真实 HTTP 状态码。
 * msg 传给 RuntimeException，保证日志与 getMessage() 可见。
 */
public class OpenAppException extends AppException {


    public OpenAppException(OpenResultCode resultCode, String msg) {
        super(msg);
        setCode(resultCode.getCode());
        setMsg(msg);
    }
}
