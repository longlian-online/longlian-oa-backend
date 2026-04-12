package online.longlian.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeGenerator {

    private static final String OUTPUT_DIR = "app\\src\\main\\java";
    private static final String ENTITY_PACKAGE_PATH = "online\\longlian\\app\\pojo\\entity";
    private static final Pattern FIELD_DECLARATION_PATTERN = Pattern.compile("private\\s+[\\w.$<>?]+\\s+(\\w+);");
    private static final Pattern LOMBOK_IMPORT_PATTERN = Pattern.compile("(?m)^import lombok\\.(Data|Builder|NoArgsConstructor|AllArgsConstructor|Getter|Setter|ToString);\\R");
    private static final Pattern LOMBOK_ANNOTATION_PATTERN = Pattern.compile("(?m)^@(Data|Builder|NoArgsConstructor|AllArgsConstructor|Getter|Setter|ToString)\\R");

    private static final Map<String, Map<String, String>> ENTITY_ENUM_FIELD_MAP = Map.ofEntries(
            Map.entry("BaseTask", Map.of("status", "online.longlian.app.common.enumeration.Status")),
            Map.entry("FileStorage", Map.of(
                    "storageType", "online.longlian.app.common.enumeration.StorageType",
                    "processStatus", "online.longlian.app.common.enumeration.FileProcessStatus"
            )),
            Map.entry("GroupApplication", Map.of("status", "online.longlian.app.common.enumeration.ApplicationStatus")),
            Map.entry("Item", Map.of("status", "online.longlian.app.common.enumeration.ItemStatus")),
            Map.entry("Organization", Map.of("status", "online.longlian.app.common.enumeration.Status")),
            Map.entry("OrganizationMember", Map.of("status", "online.longlian.app.common.enumeration.Status")),
            Map.entry("Permission", Map.of(
                    "permType", "online.longlian.app.common.enumeration.PermissionType",
                    "status", "online.longlian.app.common.enumeration.Status"
            )),
            Map.entry("Project", Map.of("status", "online.longlian.app.common.enumeration.ProjectStatus")),
            Map.entry("ProjectType", Map.of("status", "online.longlian.app.common.enumeration.Status")),
            Map.entry("Role", Map.of("status", "online.longlian.app.common.enumeration.Status")),
            Map.entry("TaskInstance", Map.of("status", "online.longlian.app.common.enumeration.TaskInstanceStatus")),
            Map.entry("TaskSubmission", Map.of("status", "online.longlian.app.common.enumeration.TaskSubmissionStatus")),
            Map.entry("TaskTemplate", Map.of(
                    "status", "online.longlian.app.common.enumeration.Status",
                    "scope", "online.longlian.app.common.enumeration.TaskTemplateScope"
            )),
            Map.entry("User", Map.of("status", "online.longlian.app.common.enumeration.Status")),
            Map.entry("UserOperationLog", Map.of("operationType", "online.longlian.app.common.enumeration.UserOperationType"))
    );

    private static final Map<String, String> LOMBOK_IMPORTS = Map.of(
            "Data", "lombok.Data",
            "Builder", "lombok.Builder",
            "NoArgsConstructor", "lombok.NoArgsConstructor",
            "AllArgsConstructor", "lombok.AllArgsConstructor"
    );

    public static void main(String[] args) {
        ConfigurableEnvironment env = loadSpringEnv();
        String url = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder.author("longlian").enableSwagger().outputDir(OUTPUT_DIR))
                .packageConfig(builder -> builder.parent("online.longlian").moduleName("app").entity("pojo.entity"))
                .strategyConfig(builder -> builder.entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .naming(com.baomidou.mybatisplus.generator.config.rules.NamingStrategy.underline_to_camel)
                        .columnNaming(com.baomidou.mybatisplus.generator.config.rules.NamingStrategy.underline_to_camel)
                        .controllerBuilder().disable()
                        .serviceBuilder().disable()
                        .mapperBuilder().enableMapperAnnotation().formatMapperFileName("%sMapper").formatXmlFileName("%sMapper"))
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        patchGeneratedEntities();
    }

    private static void patchGeneratedEntities() {
        ENTITY_ENUM_FIELD_MAP.forEach(CodeGenerator::patchEntityEnumFields);
        patchGeneratedEntityLombokAnnotations();
    }

    private static void patchGeneratedEntityLombokAnnotations() {
        Path entityDirectory = Paths.get(OUTPUT_DIR, ENTITY_PACKAGE_PATH);
        if (!Files.isDirectory(entityDirectory)) {
            return;
        }
        try (var paths = Files.list(entityDirectory)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(CodeGenerator::patchEntityLombokAnnotations);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to patch entity lombok annotations", e);
        }
    }

    private static void patchEntityLombokAnnotations(Path entityPath) {
        try {
            String content = Files.readString(entityPath, StandardCharsets.UTF_8);
            String updated = LOMBOK_IMPORT_PATTERN.matcher(content).replaceAll("");
            updated = LOMBOK_ANNOTATION_PATTERN.matcher(updated).replaceAll("");
            updated = ensureImport(updated, LOMBOK_IMPORTS.get("Data"));
            updated = ensureImport(updated, LOMBOK_IMPORTS.get("Builder"));
            updated = ensureImport(updated, LOMBOK_IMPORTS.get("NoArgsConstructor"));
            updated = ensureImport(updated, LOMBOK_IMPORTS.get("AllArgsConstructor"));
            updated = updated.replaceFirst("(?m)^@TableName", "@Data\n@Builder\n@NoArgsConstructor\n@AllArgsConstructor\n@TableName");

            if (!updated.equals(content)) {
                Files.writeString(entityPath, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to patch entity lombok annotations: " + entityPath, e);
        }
    }

    private static void patchEntityEnumFields(String entityName, Map<String, String> enumFieldMap) {
        Path entityPath = Paths.get(OUTPUT_DIR, ENTITY_PACKAGE_PATH, entityName + ".java");
        if (!Files.exists(entityPath)) {
            return;
        }
        try {
            String content = Files.readString(entityPath, StandardCharsets.UTF_8);
            String updated = content;
            Map<String, String> requiredImports = new LinkedHashMap<>();

            for (Map.Entry<String, String> entry : enumFieldMap.entrySet()) {
                String fieldName = entry.getKey();
                String enumClassName = entry.getValue();
                String simpleEnumName = simpleClassName(enumClassName);
                String replaced = replaceFieldType(updated, fieldName, simpleEnumName);
                if (!replaced.equals(updated)) {
                    updated = replaced;
                    requiredImports.put(simpleEnumName, enumClassName);
                }
            }

            if (requiredImports.isEmpty()) {
                return;
            }
            updated = ensureImports(updated, requiredImports);
            Files.writeString(entityPath, updated, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to patch generated entity: " + entityPath, e);
        }
    }

    private static String replaceFieldType(String content, String fieldName, String targetType) {
        Matcher matcher = FIELD_DECLARATION_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        boolean replaced = false;

        while (matcher.find()) {
            if (fieldName.equals(matcher.group(1))) {
                matcher.appendReplacement(result, Matcher.quoteReplacement("private " + targetType + " " + fieldName + ";"));
                replaced = true;
            }
        }
        matcher.appendTail(result);
        return replaced ? result.toString() : content;
    }

    private static String ensureImports(String content, Map<String, String> requiredImports) {
        String updated = content;
        for (String importClass : requiredImports.values()) {
            updated = ensureImport(updated, importClass);
        }
        return updated;
    }

    private static String ensureImport(String content, String importClass) {
        String importLine = "import " + importClass + ";\n";
        if (content.contains(importLine)) {
            return content;
        }
        int insertIndex = content.lastIndexOf("import ");
        if (insertIndex < 0) {
            return content;
        }
        insertIndex = content.indexOf(";", insertIndex) + 2;
        return content.substring(0, insertIndex) + importLine + content.substring(insertIndex);
    }

    private static String simpleClassName(String className) {
        int lastDotIndex = className.lastIndexOf('.');
        return lastDotIndex >= 0 ? className.substring(lastDotIndex + 1) : className;
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
