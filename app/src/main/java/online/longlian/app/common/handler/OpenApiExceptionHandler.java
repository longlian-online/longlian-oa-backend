package online.longlian.app.common.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.metrics.ApiResultMetrics;
import online.longlian.app.common.result.OpenResult;
import online.longlian.app.common.result.OpenResultCode;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 开放接口（controller.open）专用异常处理：与站内一致业务失败统一 HTTP 200，
 * 差异仅在信封（10xxx code + error 标识 + requestId）。
 * 优先级高于 GlobalExceptionHandler，作用域限定开放面 Controller，不影响站内异常语义。
 */
@Slf4j
@Order(1)
@RestControllerAdvice(basePackages = "online.longlian.app.controller.open")
@RequiredArgsConstructor
public class OpenApiExceptionHandler {

    private final ApiResultMetrics apiResultMetrics;

    @ExceptionHandler(AppException.class)
    public OpenResult<Void> handleAppException(AppException e, HttpServletRequest request) {
        OpenResultCode code = OpenResultCode.of(e.getCode());
        log.warn("开放接口业务失败 | code={} | msg={} | uri={} | method={}",
                code.getCode(), e.getMsg(), request.getRequestURI(), request.getMethod());
        return fail(request, code, e.getMsg());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public OpenResult<Void> handleValidation(MethodArgumentNotValidException e,
                                             HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(OpenResultCode.PARAM_ERROR.getMsg());
        return fail(request, OpenResultCode.PARAM_ERROR, msg);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    public OpenResult<Void> handleBadRequest(Exception e, HttpServletRequest request) {
        return fail(request, OpenResultCode.PARAM_ERROR, OpenResultCode.PARAM_ERROR.getMsg());
    }

    @ExceptionHandler(Exception.class)
    public OpenResult<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("开放接口内部异常 | uri={} | method={}", request.getRequestURI(), request.getMethod(), e);
        return fail(request, OpenResultCode.INTERNAL, OpenResultCode.INTERNAL.getMsg());
    }

    private OpenResult<Void> fail(HttpServletRequest request, OpenResultCode code, String msg) {
        apiResultMetrics.record(request, code.getCode());
        return OpenResult.fail(code, msg);
    }
}
