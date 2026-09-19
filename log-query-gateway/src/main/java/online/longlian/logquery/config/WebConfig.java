package online.longlian.logquery.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    FilterRegistrationBean<QueryAuthenticationFilter> queryAuthenticationFilter(
            LogQueryGatewayProperties properties) {
        FilterRegistrationBean<QueryAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new QueryAuthenticationFilter(properties));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(10);
        return registration;
    }
}
