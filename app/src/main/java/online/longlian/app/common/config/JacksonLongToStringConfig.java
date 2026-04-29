package online.longlian.app.common.config;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 将雪花ID字段序列化为字符串
 */
@Configuration
public class JacksonLongToStringConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonLongIdSerializerCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.setSerializerModifier(new BeanSerializerModifier() {
                @Override
                public List<BeanPropertyWriter> changeProperties(
                        SerializationConfig config,
                        BeanDescription beanDesc,
                        List<BeanPropertyWriter> beanProperties
                ) {
                    for (BeanPropertyWriter writer : beanProperties) {
                        if (isLongType(writer) && isIdField(writer.getName())) {
                            writer.assignSerializer(ToStringSerializer.instance);
                        }
                    }
                    return beanProperties;
                }
            });
            builder.modules(module);
        };
    }

    private static boolean isLongType(BeanPropertyWriter writer) {
        Class<?> rawClass = writer.getType().getRawClass();
        return Long.class.equals(rawClass) || Long.TYPE.equals(rawClass);
    }

    private static boolean isIdField(String fieldName) {
        return "id".equals(fieldName) || fieldName.endsWith("Id");
    }
}
