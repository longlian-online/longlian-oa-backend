package online.longlian.app.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 邮箱验证码认证令牌
 */
public class EmailCodeAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal; // 存储邮箱
    private Object credentials;     // 存储验证码

    // 1. 认证前构造器：未认证状态
    public EmailCodeAuthenticationToken(String email, String code) {
        super(null);
        this.principal = email;
        this.credentials = code;
        setAuthenticated(false);
    }

    // 2. 认证后构造器：已认证状态
    public EmailCodeAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException("不能手动设置为已认证，请使用认证后构造器");
        }
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}