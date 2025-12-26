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
                .strategyConfig(builder -> builder
                        .addInclude("user")
                        .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .controllerBuilder()
                        .disable()
                )
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