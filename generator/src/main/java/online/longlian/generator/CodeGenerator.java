package online.longlian.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;
import online.longlian.generator.internal.EnumFieldMeta;
import online.longlian.generator.internal.EnumProcessor;
import online.longlian.generator.internal.ModelEnumMeta;
import online.longlian.generator.internal.TypeConverter;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CodeGenerator {

    private static final String OUTPUT_DIR = Paths.get("app", "src", "main", "java").toString();
    private static Map<EnumFieldMeta, ModelEnumMeta> enumTypeConvertMap = new LinkedHashMap<>();

    public static Map<EnumFieldMeta, ModelEnumMeta> getEnumTypeConvertMap() {
        return Collections.unmodifiableMap(enumTypeConvertMap);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        StandardEnvironment env = loadApplicationYaml();
        String url = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password", "");
        if (!StringUtils.hasText(url) || username == null) {
            throw new IllegalStateException(
                    "配置文件缺少 spring.datasource.url / spring.datasource.username，请检查 application.yml");
        }

        // 获取枚举映射 表名 + 字段名 -> 类型名 + 包名
        enumTypeConvertMap = EnumProcessor.scanAndPrintModelEnums();

        FastAutoGenerator.create(new DataSourceConfig
                        .Builder(url, username, password)
                        .typeConvertHandler(new TypeConverter())
                )
                .globalConfig(builder -> builder.author("longlian")
                        .enableSwagger().outputDir(OUTPUT_DIR).disableOpenDir().commentDate(""))
                .packageConfig(builder -> builder.parent("online.longlian").moduleName("app").entity("pojo.entity"))
                .strategyConfig(builder -> builder.entityBuilder()
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

    private static StandardEnvironment loadApplicationYaml() throws IOException {
        Path yml = resolveApplicationYml();
        Resource resource = new FileSystemResource(yml);
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("application", resource);
        if (sources.isEmpty()) {
            throw new IllegalStateException("配置文件为空: " + yml.toAbsolutePath());
        }
        StandardEnvironment env = new StandardEnvironment();
        for (PropertySource<?> source : sources) {
            env.getPropertySources().addFirst(source);
        }
        return env;
    }

    private static Path resolveApplicationYml() {
        Path[] candidates = {
                Paths.get("app/src/main/resources/application.yml"),
                Paths.get("../app/src/main/resources/application.yml"),
                Paths.get("app/src/main/resources/application.yml.example"),
                Paths.get("../app/src/main/resources/application.yml.example"),
        };
        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        throw new IllegalStateException(
                "找不到应用配置。请复制 app/src/main/resources/application.yml.example 为 application.yml");
    }
}
