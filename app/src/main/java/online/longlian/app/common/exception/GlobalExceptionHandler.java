package online.longlian.app.common.exception;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 全局统一异常处理
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常（AppException）
     */
    @ExceptionHandler(value = AppException.class)
    @ResponseBody
    public <T> Result<T> appExceptionHandler(Exception e) {
        if(e instanceof AppException appException) {
            return Result.fail(appException.getCode(),appException.getMsg());
        }
        return Result.fail(ResultCode.FAIL);
    }
}