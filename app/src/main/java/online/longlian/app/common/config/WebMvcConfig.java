package online.longlian.app.common.config;

import lombok.RequiredArgsConstructor;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import online.longlian.app.common.constants.PatternConstants;
import online.longlian.app.common.resolver.UserSessionArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserSessionArgumentResolver userSessionArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userSessionArgumentResolver);
    }

    /**
     * HTTP JSON 统一使用 Fastjson2，避免同一接口因返回值类型切换序列化实现。
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter fastJsonConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setCharset(StandardCharsets.UTF_8);
        fastJsonConfig.setDateFormat(PatternConstants.TIME_PATTERN);
        fastJsonConverter.setFastJsonConfig(fastJsonConfig);
        fastJsonConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));

        converters.removeIf(converter -> converter instanceof MappingJackson2HttpMessageConverter);

        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof StringHttpMessageConverter) {
                // Fastjson2 必须排在 String 转换器前，才能包装原始 String 返回值。
                converters.add(i, fastJsonConverter);
                return;
            }
        }
        converters.add(fastJsonConverter);
    }
}
