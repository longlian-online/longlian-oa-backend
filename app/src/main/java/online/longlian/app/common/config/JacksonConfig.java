package online.longlian.app.common.config;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.app.common.constants.PatternConstants;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
public class JacksonConfig {

    /**
     * JSON 序列化配置，将显式标记的雪花ID字段序列化为字符串，序列化时间
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addSerializer(LocalDateTime.class,
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(PatternConstants.TIME_PATTERN)));

            SimpleModule longIdModule = new SimpleModule();
            longIdModule.setSerializerModifier(new BeanSerializerModifier() {
                @Override
                public List<BeanPropertyWriter> changeProperties(
                        SerializationConfig config,
                        BeanDescription beanDesc,
                        List<BeanPropertyWriter> beanProperties
                ) {
                    for (BeanPropertyWriter writer : beanProperties) {
                        if (isJsonLongIdString(writer, beanDesc)) {
                            writer.assignSerializer(ToStringSerializer.instance);
                        }
                    }
                    return beanProperties;
                }
            });

            builder.modules(javaTimeModule, longIdModule);
        };
    }

    private static boolean isJsonLongIdString(BeanPropertyWriter writer, BeanDescription beanDesc) {
        Class<?> rawClass = writer.getType().getRawClass();
        if (!Long.class.equals(rawClass) && !Long.TYPE.equals(rawClass)) {
            return false;
        }

        for (BeanPropertyDefinition property : beanDesc.findProperties()) {
            if (writer.getName().equals(property.getName())) {
                return hasJsonLongIdString(property.getField()) || hasJsonLongIdString(property.getGetter());
            }
        }
        return false;
    }

    private static boolean hasJsonLongIdString(AnnotatedMember member) {
        return member != null && member.hasAnnotation(JsonLongIdString.class);
    }

}
