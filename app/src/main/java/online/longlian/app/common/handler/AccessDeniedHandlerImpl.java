package online.longlian.app.common.handler;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.metrics.ApiResultMetrics;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import online.longlian.app.common.result.OpenResult;
import online.longlian.app.common.result.OpenResultCode;
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
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp}")
    private String mcpEndpoint = "/mcp";

    private final ApiResultMetrics apiResultMetrics;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        if (request.getServletPath().equals(mcpEndpoint)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }
        // 开放接口与站内一致：业务失败统一 HTTP 200，code 用 10xxx 区分
        if (request.getServletPath().startsWith("/open/")) {
            response.setContentType(CommonConstants.CONTENT_TYPE);
            response.setStatus(HttpStatus.OK.value());
            apiResultMetrics.record(request, OpenResultCode.FORBIDDEN.getCode());
            response.getWriter().write(JSON.toJSONString(OpenResult.fail(OpenResultCode.FORBIDDEN)));
            return;
        }

        // 日志打印
        log.warn("URL权限不足 |msg={} | uri={} | method={}",
                accessDeniedException.getMessage(),
                request.getRequestURI(),
                request.getMethod()
        );

        // 设置响应状态和头
        response.setContentType(CommonConstants.CONTENT_TYPE);
        response.setStatus(HttpStatus.OK.value());

        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED_OPERATION);
        apiResultMetrics.record(request, result.getCode());
        response.getWriter().write(JSON.toJSONString(result));
    }
}
