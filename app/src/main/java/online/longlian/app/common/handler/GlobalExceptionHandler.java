package online.longlian.app.common.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    @ResponseBody
    public <T> Result<T> handleAppException(AppException appException, HttpServletRequest request) {
        log.warn("业务异常 | code={} | msg={} | uri={} | method={}",
                appException.getCode(),
                appException.getMsg(),
                request.getRequestURI(),
                request.getMethod()
        );
        return Result.fail(appException.getCode(), appException.getMsg());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseBody
    public <T> Result<T> handleAuthorizationDeniedException(AuthorizationDeniedException e, HttpServletRequest request) {
        log.warn("方法权限不足 | msg={} | uri={} | method={}",
                e.getMessage(),
                request.getRequestURI(),
                request.getMethod());
        return Result.fail(ResultCode.UNAUTHORIZED_OPERATION);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public <T> Result<T> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {

        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .collect(Collectors.joining("；"));

        log.warn("参数校验失败 | msg={} | uri={} | method={}",
                errorMsg,
                request.getRequestURI(),
                request.getMethod());

        return Result.fail(ResultCode.PARAM_ERROR.getCode(), errorMsg);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseBody
    public <T> Result<T> handleNotFoundException(Exception e, HttpServletRequest request) {
        log.warn("请求资源不存在 | uri={} | method={}",
                request.getRequestURI(),
                request.getMethod());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public <T> Result<T> handleException(Exception exception, HttpServletRequest request) {
        log.error("系统内部异常 | uri={} | method={}",
                request.getRequestURI(),
                request.getMethod(),
                exception);
        return Result.fail(ResultCode.FAIL);
    }
}