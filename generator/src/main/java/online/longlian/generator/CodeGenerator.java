package online.longlian.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;
import com.baomidou.mybatisplus.generator.type.ITypeConvertHandler;
import com.baomidou.mybatisplus.generator.type.TypeRegistry;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import java.util.Properties;

class EnumClass implements IColumnType {
    @Override
    public String getType() {
        return "Integer";
    }

    @Override
    public String getPkg() {
        return (String)null;
    }
}
class TypeConverter implements ITypeConvertHandler {
    @Override
    public IColumnType convert(GlobalConfig globalConfig, TypeRegistry typeRegistry, TableField.MetaInfo metaInfo) {
        if (metaInfo.getTypeName().equals("TINYINT")) {
            return new EnumClass();
        }

        return typeRegistry.getColumnType(metaInfo);
    }
}

public class CodeGenerator {

    private static final String OUTPUT_DIR = "app\\src\\main\\java";

    public static void main(String[] args) {
        ConfigurableEnvironment env = loadSpringEnv();
        String url = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        FastAutoGenerator.create(new DataSourceConfig
                        .Builder(url, username, password)
                        .typeConvertHandler(new TypeConverter())
                )
                .globalConfig(builder -> builder.author("longlian").enableSwagger().outputDir(OUTPUT_DIR).disableOpenDir())
                .packageConfig(builder -> builder.parent("online.longlian").moduleName("app").entity("pojo.entity"))
                .strategyConfig(builder -> builder.entityBuilder()
                        .enableFileOverride()
                        .enableLombok(
                                new ClassAnnotationAttributes("@Data","lombok.Data"),
                                new ClassAnnotationAttributes("@Builder", "lombok.Builder"),
                                new ClassAnnotationAttributes("@NoArgsConstructor", "lombok.NoArgsConstructor"),
                                new ClassAnnotationAttributes("@AllArgsConstructor", "lombok.AllArgsConstructor")
                        )
                        .enableTableFieldAnnotation()
                        .naming(com.baomidou.mybatisplus.generator.config.rules.NamingStrategy.underline_to_camel)
                        .columnNaming(com.baomidou.mybatisplus.generator.config.rules.NamingStrategy.underline_to_camel)
                        .controllerBuilder().disable()
                        .serviceBuilder()
                        .disable()
                        .mapperBuilder()
                        .enableMapperAnnotation()
                        .formatMapperFileName("%sMapper")
                        .formatXmlFileName("%sMapper"))
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

    }

    private static ConfigurableEnvironment loadSpringEnv() {
        AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
        ConfigurableEnvironment env = context.getEnvironment();
        loadYaml(env, "app/src/main/resources/application.yml", "appYaml");
        return env;
    }

    private static void loadYaml(ConfigurableEnvironment env, String path, String name) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource(path));
        Properties properties = yaml.getObject();
        env.getPropertySources().addLast(new PropertiesPropertySource(name, properties));
    }
}
