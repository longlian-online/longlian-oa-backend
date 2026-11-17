package online.longlian.app.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.RequireCaptcha;
import online.longlian.app.common.enumeration.CaptchaAction;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.CaptchaProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.CaptchaVerifyParamsBO;
import online.longlian.app.service.captcha.CaptchaService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 读取实际匹配的 HandlerMethod 注解做验票，不另维护 URL 表。
 * 异步分派会重复回调 preHandle；票据只能在当前 HTTP 请求内复用校验结果。
 */
@Component
@RequiredArgsConstructor
public class CaptchaInterceptor implements HandlerInterceptor {
    public static final String TOKEN_HEADER = "X-Captcha-Token";
    private static final String VERIFIED_ATTRIBUTE = CaptchaInterceptor.class.getName() + ".verified";

    private final CaptchaProperties properties;
    private final CaptchaService captchaService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled() || !(handler instanceof HandlerMethod method)) return true;
        RequireCaptcha requirement = method.getMethodAnnotation(RequireCaptcha.class);
        if (requirement == null) return true;
        CaptchaAction action = requirement.value();
        if (request.getAttribute(VERIFIED_ATTRIBUTE) == action) return true;
        String token = request.getHeader(TOKEN_HEADER);
        if (!StringUtils.hasText(token)) throw new AppException(ResultCode.CAPTCHA_REQUIRED);
        captchaService.verify(CaptchaVerifyParamsBO.builder()
                .token(token)
                .action(action)
                .remoteIp(properties.isBindIp() ? request.getRemoteAddr() : null)
                .build());
        request.setAttribute(VERIFIED_ATTRIBUTE, action);
        return true;
    }
}
