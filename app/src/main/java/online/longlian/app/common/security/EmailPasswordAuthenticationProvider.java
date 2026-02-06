package online.longlian.app.common.security;


import lombok.RequiredArgsConstructor;
import online.longlian.app.common.result.ResultCode;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 邮箱密码认证提供者：实现邮箱+密码的认证逻辑
 */
@Component
@RequiredArgsConstructor
public class EmailPasswordAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    /**
     * 核心认证方法
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // 1. 从认证令牌中获取邮箱和密码
        String email = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        // 2. 按邮箱查询用户（调用自定义UserDetailsService）
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // 3. 验证密码
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException(ResultCode.USERNAME_OR_PASSWORD_ERROR.getMsg());
        }

        // 4. 认证成功：返回已认证的令牌（封装UserDetails和权限）
        return new EmailPasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    /**
     * 声明当前提供者支持的认证令牌类型
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return EmailPasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}