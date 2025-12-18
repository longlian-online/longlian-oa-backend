package online.longlian.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * 自定义 OpenAPI 文档信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 文档基本信息
                .info(new Info()
                        .title("longlian接口文档") // 文档标题
                        .description("longlian的接口文档示例") // 文档描述
                        .version("v1.0.0")
                        // 联系人信息
                        .contact(new Contact()
                                .name("longlian")));
    }
}