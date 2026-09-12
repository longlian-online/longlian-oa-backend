package online.longlian.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;
import online.longlian.generator.internal.EnumFieldMeta;
import online.longlian.generator.internal.EnumProcessor;
import online.longlian.generator.internal.ModelEnumMeta;
import online.longlian.generator.internal.TypeConverter;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CodeGenerator {

    private static final String OUTPUT_DIR = Paths.get("app", "src", "main", "java").toString();
    private static Map<EnumFieldMeta, ModelEnumMeta> enumTypeConvertMap = new LinkedHashMap<>();

    public static Map<EnumFieldMeta, ModelEnumMeta> getEnumTypeConvertMap() {
        return Collections.unmodifiableMap(enumTypeConvertMap);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        StandardEnvironment env = new StandardEnvironment();
        String host = env.getProperty("DB_HOST");
        String port = env.getProperty("DB_PORT");
        String database = env.getProperty("DB_DATABASE");
        String username = env.getProperty("DB_USERNAME");
        String password = env.getProperty("DB_PASSWORD");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";

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
}
