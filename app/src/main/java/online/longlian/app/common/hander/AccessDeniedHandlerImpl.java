package online.longlian.app.common.hander;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 自定义 URL 级权限不足处理器
 */
@Slf4j
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // 获取 traceId
        String traceId = MDC.get(CommonConstants.TRACE_ID);

        // 日志打印
        log.warn("URL权限不足 | traceId={} | msg={} | uri={} | method={}",
                traceId,
                accessDeniedException.getMessage(),
                request.getRequestURI(),
                request.getMethod()
        );

        // 设置响应状态和头
        response.setContentType(CommonConstants.CONTENT_TYPE);
        response.setStatus(HttpStatus.FORBIDDEN.value());

        // 封装统一返回
        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED_OPERATION);

        // 写入响应体
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
