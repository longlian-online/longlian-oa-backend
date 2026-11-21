package online.longlian.app.common.handler;

import online.longlian.app.common.result.OpenResult;
import online.longlian.app.common.result.Result;
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
 * 追加 requestId。开放面错误统一走 OpenApiExceptionHandler（真实 HTTP 状态码），
 * 正常返回只会见到成功信封，因此这里只做成功转换。在 @Order(100) 的 ResultResponseBodyAdvice 之后执行。
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
        // 包装 advice 对 controller.open 先行产出 Result；其他形态说明链路被破坏，按 500 兜底
        Result<?> result = (Result<?>) body;
        return OpenResult.of(result.getCode(), result.getMsg(), result.getData(), null);
    }
}
