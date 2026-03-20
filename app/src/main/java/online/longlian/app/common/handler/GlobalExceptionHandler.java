package online.longlian.app.common.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import org.slf4j.MDC;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    @ResponseBody
    public <T> Result<T> handleAppException(AppException appException, HttpServletRequest request) {
        // 从 MDC 获取 traceId
        String traceId = MDC.get(CommonConstants.TRACE_ID);
        log.warn("业务异常 | traceId={} | code={} | msg={} | uri={} | method={}",
                traceId,
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
        String traceId = MDC.get("traceId");
        log.warn("方法权限不足 | traceId={} | msg={} | uri={} | method={}",
                traceId,
                e.getMessage(),
                request.getRequestURI(),
                request.getMethod());
        return Result.fail(ResultCode.UNAUTHORIZED_OPERATION);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public <T> Result<T> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String traceId = MDC.get(CommonConstants.TRACE_ID);

        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .collect(Collectors.joining("；"));

        log.warn("参数校验失败 | traceId={} | msg={} | uri={} | method={}",
                traceId,
                errorMsg,
                request.getRequestURI(),
                request.getMethod());

        return Result.fail(ResultCode.PARAM_ERROR.getCode(), errorMsg);
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public <T> Result<T> handleException(Exception exception, HttpServletRequest request) {
        // 从 MDC 获取 traceId
        String traceId = MDC.get(CommonConstants.TRACE_ID);
        log.error("系统内部异常 | traceId={} | uri={} | method={}",
                traceId,
                request.getRequestURI(),
                request.getMethod(),
                exception);
        return Result.fail(ResultCode.FAIL);
    }
}