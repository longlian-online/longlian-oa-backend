package online.longlian.app.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import online.longlian.app.common.constants.CommonConstants;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static online.longlian.app.common.util.TraceIdUtil.getTraceId;

@Component
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        String traceId = getTraceId();

        // 将 traceId 写入响应头返回给前端
        if (response instanceof HttpServletResponse) {
            ((HttpServletResponse) response).setHeader(CommonConstants.RESPONSE_HEADER_TRACE_ID, traceId);
        }

        chain.doFilter(request, response);
    }
}
