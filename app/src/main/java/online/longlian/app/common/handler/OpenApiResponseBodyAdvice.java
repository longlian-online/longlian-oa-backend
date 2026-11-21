package online.longlian.app.common.handler;

import online.longlian.app.common.result.OpenResult;
import online.longlian.app.common.result.OpenResultCode;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 开放接口（controller.open）的响应后置处理：把包装 advice 产出的 Result 转为 OpenResult，
 * 追加 error 标识与 requestId，并按错误段写真实 HTTP 状态码。
 * 在 @Order(100) 的 ResultResponseBodyAdvice 之后执行。
 */
@Order(200)
@ControllerAdvice(basePackages = "online.longlian.app.controller.open")
public class OpenApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof Result<?> result)) {
            // 正常路径包装 advice 先行，这里只是兜底
            return OpenResult.success(body);
        }
        if (result.getCode() != ResultCode.SUCCESS.getCode()) {
            // 站内码不经开放面外泄，映射到最近的开放语义
            OpenResultCode mapped = OpenResultCode.of(result.getCode());
            response.setStatusCode(mapped.getHttpStatus());
            return OpenResult.of(result.getCode(), result.getMsg(), null, mapped.getError());
        }
        return OpenResult.of(result.getCode(), result.getMsg(), result.getData(), null);
    }
}
