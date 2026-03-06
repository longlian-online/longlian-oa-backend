package online.longlian.app.common.security;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.service.VerifyCodeService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 邮箱验证码认证提供者：实现邮箱+验证码的认证逻辑
 */
@Component
@RequiredArgsConstructor
public class EmailCodeAuthenticationProvider implements AuthenticationProvider {
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final VerifyCodeService verifyCodeService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // 1. 获取邮箱和验证码
        String email = (String) authentication.getPrincipal();
        String code = (String) authentication.getCredentials();

        // 2. 验证验证码
        if (!verifyCodeService.validateCode(email, code)) {
            throw new AppException(ResultCode.OPERATION_FAIL,"验证码错误或已过期");
        }

        // 3. 按邮箱查询用户
        UserDetails userDetails = userDetailsServiceImpl.loadUserByEmailOnly(email);

        // 4. 认证成功：返回已认证令牌
        return new EmailCodeAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return EmailCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}