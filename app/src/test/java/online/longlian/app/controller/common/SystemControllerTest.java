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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemControllerTest {

    /** 使用实际构建产物与 Fastjson2 验证统一包装中的发布元数据。 */
    @Test
    void shouldReturnWrappedBuildMetadata() throws Exception {
        Properties properties = new Properties();
        try (var input = getClass().getResourceAsStream("/META-INF/build-info.properties")) {
            properties.load(input);
        }
        Properties buildProperties = new Properties();
        buildProperties.setProperty("version", properties.getProperty("build.version"));
        buildProperties.setProperty("time", properties.getProperty("build.time"));
        var service = new SystemInfoServiceImpl(new BuildProperties(buildProperties), "测试 <OA> & 团队");
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
                .andExpect(jsonPath("$.data.displayName").value("测试 <OA> & 团队"))
                .andExpect(jsonPath("$.data.version").value(properties.getProperty("build.version")))
                .andExpect(jsonPath("$.data.buildTime").value(new BuildProperties(buildProperties).getTime().toString()));
    }
}
