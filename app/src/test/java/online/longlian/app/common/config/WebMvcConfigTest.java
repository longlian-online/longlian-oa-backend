package online.longlian.app.common.config;

import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import online.longlian.app.common.resolver.UserSessionArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebMvcConfigTest {

    @Test
    void shouldUseFastjsonBeforeStringConverter() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>(List.of(
                new StringHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter()
        ));

        new WebMvcConfig(null).extendMessageConverters(converters);

        assertThat(converters).first().isInstanceOf(FastJsonHttpMessageConverter.class);
        assertThat(converters).noneMatch(MappingJackson2HttpMessageConverter.class::isInstance);
        assertThat(converters).anyMatch(StringHttpMessageConverter.class::isInstance);
        FastJsonHttpMessageConverter fastJson = (FastJsonHttpMessageConverter) converters.get(0);
        assertThat(fastJson.getSupportedMediaTypes()).containsExactly(MediaType.APPLICATION_JSON);
    }

    @Test
    void shouldRegisterSessionResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        UserSessionArgumentResolver resolver = mock(UserSessionArgumentResolver.class);

        new WebMvcConfig(resolver).addArgumentResolvers(resolvers);

        assertThat(resolvers).containsExactly(resolver);
    }

    @Test
    void shouldAppendFastjsonWhenStringConverterIsMissing() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();

        new WebMvcConfig(null).extendMessageConverters(converters);

        assertThat(converters).singleElement().isInstanceOf(FastJsonHttpMessageConverter.class);
    }
}
