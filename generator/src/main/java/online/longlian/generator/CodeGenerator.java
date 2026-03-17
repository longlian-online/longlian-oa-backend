package online.longlian.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;

import java.util.Properties;

public class CodeGenerator {

    public static void main(String[] args) {
        ConfigurableEnvironment env = loadSpringEnv();

        String url = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder
                        .author("longlian")
                        .enableSwagger()
                        .outputDir("app\\src\\main\\java")
                )
                .packageConfig(builder -> builder
                        .parent("online.longlian")
                        .moduleName("app")
                        .entity("pojo.entity")
                )
                .strategyConfig(builder -> {
                    builder.addInclude(
                                    "base_task",
                                    "file_storage",
                                    "group_application",
                                    "item",
                                    "item_task_flow",
                                    "item_task_node",
                                    "organization",
                                    "organization_member",
                                    "permission",
                                    "project",
                                    "project_type",
                                    "project_workshop",
                                    "role",
                                    "role_permission",
                                    "task_instance",
                                    "task_submission",
                                    "task_template",
                                    "task_template_node",
                                    "user",
                                    "user_operation_log",
                                    "user_role"
                            )
                            .entityBuilder()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .naming(com.baomidou.mybatisplus.generator.config.rules.NamingStrategy.underline_to_camel)
                            .columnNaming(com.baomidou.mybatisplus.generator.config.rules.NamingStrategy.underline_to_camel)

                            .controllerBuilder()
                            .disable()
                            .mapperBuilder()
                            .enableMapperAnnotation()
                            .formatMapperFileName("%sMapper")
                            .formatXmlFileName("%sMapper");
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    private static ConfigurableEnvironment loadSpringEnv() {
        AnnotationConfigServletWebServerApplicationContext context =
                new AnnotationConfigServletWebServerApplicationContext();

        ConfigurableEnvironment env = context.getEnvironment();
        env.setActiveProfiles("dev");

        loadYaml(env, "app/src/main/resources/application-dev.yml", "devYaml");

        loadYaml(env, "app/src/main/resources/application.yml", "appYaml");

        return env;
    }

    private static void loadYaml(ConfigurableEnvironment env, String path, String name) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource(path));
        Properties properties = yaml.getObject();
        env.getPropertySources().addLast(
                new PropertiesPropertySource(name, properties)
        );
    }
}