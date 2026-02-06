package online.longlian.app.common.hander;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * 自定义认证异常处理器：处理未认证、认证失败相关异常
 */
@Slf4j
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        // 获取 traceId（如果有使用 MDC）
        String traceId = MDC.get(CommonConstants.TRACE_ID);
        String msg = (authException != null) ? authException.getMessage() : "未认证访问";
        log.warn("认证失败 | traceId={} | msg={} | uri={} | method={}",
                traceId,
                msg,
                request.getRequestURI(),
                request.getMethod()
        );

        // 设置响应头（JSON格式、编码）
        response.setContentType(CommonConstants.CONTENT_TYPE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        // 封装统一响应
        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED);

        // 写入响应体
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
