package online.longlian.app.common.config;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.SecurityConstants;
import online.longlian.app.common.filter.JwtAuthenticationFilter;
import online.longlian.app.common.filter.TraceIdFilter;
import online.longlian.app.common.security.EmailCodeAuthenticationProvider;
import online.longlian.app.common.security.MyUsernamePasswordAuthenticationProvider;
import online.longlian.app.common.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final TraceIdFilter traceIdFilter;
    private final MyUsernamePasswordAuthenticationProvider emailPasswordProvider;
    private final EmailCodeAuthenticationProvider emailCodeProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable)
                //无状态 Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                //请求授权配置
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.getPermitAllMatchers().toArray(new RequestMatcher[0])).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                //添加 JWT 过滤器
                .addFilterBefore(traceIdFilter,UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService);
        // 加入自定义的两个认证提供者
        builder.authenticationProvider(emailPasswordProvider);
        builder.authenticationProvider(emailCodeProvider);
        return builder.build();
    }

}

