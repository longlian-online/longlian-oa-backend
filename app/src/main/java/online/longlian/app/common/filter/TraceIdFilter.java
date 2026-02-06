package online.longlian.app.common.filter;

import jakarta.servlet.*;
import online.longlian.app.common.constants.CommonConstants;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        try {
            // 生成 TraceId
            String traceId = UUID.randomUUID().toString().replace("-", "");
            // 放入 MDC
            MDC.put(CommonConstants.TRACE_ID, traceId);

            chain.doFilter(request, response);
        } finally {
            MDC.remove(CommonConstants.TRACE_ID);
        }
    }
}
