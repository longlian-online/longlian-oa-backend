package online.longlian.app.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class WebLogAspect {
    private static final String TRACE_ID = "traceId";
    /**
     * 拦截 Controller 的所有 public 方法
     * 仅打印请求开始/结束日志及耗时
     */
    @Around("execution(public * online.longlian.app..controller..*(..))")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        // 获取请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs.getRequest();
        String traceId = MDC.get(TRACE_ID);
        // 打印请求开始日志
        Object[] args = joinPoint.getArgs();
        log.info("请求开始 | traceId={} | uri={} | method={} | classMethod={} | args={}",
                traceId,
                request.getRequestURI(),
                request.getMethod(),
                joinPoint.getSignature(),
                args
        );
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - startTime;
        // 打印请求结束日志
        log.info("请求结束 | traceId={} | uri={} | method={} | classMethod={} | elapsed={}ms | response={}",
                traceId,
                request.getRequestURI(),
                request.getMethod(),
                joinPoint.getSignature(),
                elapsed,
                result
        );
        return result;
    }
}
