package online.longlian.app.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 邮箱密码认证令牌
 */
public class EmailPasswordAuthenticationToken extends AbstractAuthenticationToken {
    // 存储邮箱（认证前：principal为邮箱，credentials为密码；认证后：principal为UserDetails）
    private final Object principal;
    // 存储密码
    private Object credentials;

    // 1. 认证前构造器：未认证状态
    public EmailPasswordAuthenticationToken(String email, String password) {
        super(null);
        this.principal = email;
        this.credentials = password;
        setAuthenticated(false); // 必须手动设置为未认证
    }

    // 2. 认证后构造器：已认证状态（由AuthenticationProvider调用）
    public EmailPasswordAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true); // 已认证
    }

    // 重写抽象方法
    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    // 重写setAuthenticated：防止手动修改认证状态
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