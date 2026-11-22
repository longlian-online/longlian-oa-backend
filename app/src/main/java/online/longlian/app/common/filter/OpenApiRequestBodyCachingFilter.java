package online.longlian.app.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import online.longlian.app.common.constants.OpenApiConstants;
import online.longlian.app.common.util.Sha256Util;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * /open/** 写请求的请求体缓存：先读原始报文计算 SHA-256 存入请求属性（幂等哈希），
 * 再以可重复读的包装请求继续链路，@RequestBody 反序列化不受影响。
 * 幂等哈希必须取原始报文，不能拿反序列化后的 DTO 重组。
 */
@Component
public class OpenApiRequestBodyCachingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(OpenApiConstants.OPEN_PATH_PREFIX)
                || HttpMethod.GET.matches(request.getMethod())
                || HttpMethod.HEAD.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        byte[] body = request.getInputStream().readAllBytes();
        request.setAttribute(OpenApiConstants.BODY_HASH_ATTRIBUTE, Sha256Util.sha256Hex(body));
        filterChain.doFilter(new OpenApiCachedBodyRequestWrapper(request, body), response);
    }
}
