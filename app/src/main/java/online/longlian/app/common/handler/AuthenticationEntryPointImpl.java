package online.longlian.app.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;

/**
 * 自定义认证异常处理器：处理未认证、认证失败相关异常
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {


        try {
            HandlerExecutionChain handlerExecutionChain = requestMappingHandlerMapping.getHandler(request);
            if (handlerExecutionChain == null) {
                log.warn("请求资源不存在 | uri={} | method={}",
                        request.getRequestURI(),
                        request.getMethod());
                response.setContentType(CommonConstants.CONTENT_TYPE);
                response.setStatus(HttpStatus.OK.value());
                response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ResultCode.NOT_FOUND)));
                return;
            }
        } catch (Exception e) {
            log.error("获取对应 handler 失败, err: {}", e.getMessage());
            response.setContentType(CommonConstants.CONTENT_TYPE);
            response.setStatus(HttpStatus.OK.value());
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ResultCode.FAIL)));
            return;
        }

        String msg = (authException != null) ? authException.getMessage() : "未认证访问";
        log.warn("认证失败 | msg={} | uri={} | method={}",
                msg,
                request.getRequestURI(),
                request.getMethod()
        );
        response.setContentType(CommonConstants.CONTENT_TYPE);
        response.setStatus(HttpStatus.OK.value());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ResultCode.UNAUTHORIZED)));
    }
}
