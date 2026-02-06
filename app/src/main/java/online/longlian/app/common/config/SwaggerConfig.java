package online.longlian.app.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * 自定义 OpenAPI 文档信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // 1. 定义安全方案（Token 请求头）
        Components components = new Components()
                .addSecuritySchemes("Authorization",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("Bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                );

        return new OpenAPI()
                .info(new Info()
                        .title("longlian接口文档")
                        .description("longlian的接口文档示例")
                        .version("v1.0.0")
                        .contact(new Contact().name("longlian")))
                .components(components)
                // 全局启用 Token 授权（所有接口默认携带 Token 请求头）
                .addSecurityItem(new SecurityRequirement().addList("Authorization"));

    }
}