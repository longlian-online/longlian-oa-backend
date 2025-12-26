package online.longlian.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;

import java.util.HashMap;
import java.util.Map;
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
                        .outputDir(System.getProperty("user.dir")) // 全局输出目录
                )
                .packageConfig(builder -> {
                    builder.parent("online.longlian") // 公共父包
                            .entity("pojo.entity")
                            .mapper("dao.mapper")
                            .service("service") // 接口包
                            .serviceImpl("service.impl"); // 实现类包

                    Map<OutputFile, String> pathInfo = new HashMap<>();
                    pathInfo.put(OutputFile.entity, System.getProperty("user.dir") + "/longlian-pojo/src/main/java/online/longlian/pojo/entity");
                    pathInfo.put(OutputFile.mapper, System.getProperty("user.dir") + "/longlian-dao/src/main/java/online/longlian/dao/mapper");
                    pathInfo.put(OutputFile.service, System.getProperty("user.dir") + "/longlian-service/src/main/java/online/longlian/service");
                    pathInfo.put(OutputFile.serviceImpl, System.getProperty("user.dir") + "/longlian-service/src/main/java/online/longlian/service/impl");
                    builder.pathInfo(pathInfo);
                })
                .strategyConfig(builder -> builder
                        .addInclude("user")   // 需要生成的表
                        .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .disableSerialVersionUID()
                        .controllerBuilder().disable() // 禁用Controller生成
                        .serviceBuilder() // Service生成策略
                        .formatServiceFileName("I%sService") // 接口命名
                        .formatServiceImplFileName("%sServiceImpl") // 实现类命名
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
