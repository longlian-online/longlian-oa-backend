package online.longlian.app.controller.common;

import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import online.longlian.app.common.handler.ResultResponseBodyAdvice;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.service.common.impl.SystemInfoServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Properties;
import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemControllerTest {

    /** 使用实际构建产物与 Fastjson2 验证路由和统一响应包装，防止构建版本未写入。 */
    @Test
    void shouldReturnWrappedBuildVersion() throws Exception {
        Properties properties = new Properties();
        try (var input = getClass().getResourceAsStream("/META-INF/build-info.properties")) {
            properties.load(input);
        }
        Properties buildProperties = new Properties();
        buildProperties.setProperty("version", properties.getProperty("build.version"));
        var service = new SystemInfoServiceImpl(new BuildProperties(buildProperties));
        var converter = new FastJsonHttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SystemController(service))
                .setControllerAdvice(new ResultResponseBodyAdvice())
                .setMessageConverters(converter)
                .build();

        mvc.perform(get("/common/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.msg").isString())
                .andExpect(jsonPath("$.data", aMapWithSize(1)))
                .andExpect(jsonPath("$.data.version").value(properties.getProperty("build.version")));
    }
}
