package online.longlian.app.common.handler;

import com.alibaba.fastjson2.JSON;
import online.longlian.app.common.annotation.NotWrap;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 自动将 Controller 返回值包装为 {@link Result}，消除每个接口手动调用 Result.success() 的样板代码
 * <p>
 * 兼容策略：若 body 已是 Result 实例（如 GlobalExceptionHandler 返回的失败结果或尚未迁移的旧代码），
 * 直接返回不做二次包装，确保现有代码平滑过渡
 */
@ControllerAdvice(basePackages = "online.longlian.app.controller")
public class ResultResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    /** 未标注 @ResponseMessage 时的兜底消息 */
    private static final String DEFAULT_SUCCESS_MSG = ResultCode.SUCCESS.getMsg();

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (returnType.hasMethodAnnotation(NotWrap.class)
                || returnType.getContainingClass().isAnnotationPresent(NotWrap.class)) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                   MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        // 已是 Result 则跳过（兼容旧 Controller 和异常处理器）
        if (body instanceof Result) {
            return body;
        }

        String msg = DEFAULT_SUCCESS_MSG;
        if (returnType.hasMethodAnnotation(ResponseMessage.class)) {
            ResponseMessage annotation = returnType.getMethodAnnotation(ResponseMessage.class);
            if (annotation != null) {
                msg = annotation.value();
            }
        }

        Result<?> result;
        if (body == null) {
            result = Result.success(msg);
        } else {
            result = Result.success(msg, body);
        }

        // 控制器返回 String 时，Spring 会提前选择 StringHttpMessageConverter
        // beforeBodyWrite 返回 Result 后该转换器无法处理，导致 ClassCastException
        // 此处将 Result 序列化为 JSON 字符串，兼容 StringHttpMessageConverter
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return JSON.toJSONString(result);
        }

        return result;
    }
}
