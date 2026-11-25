package com.limemall.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 序列化配置。
 * 统一日期时间格式：LocalDateTime -> yyyy-MM-dd HH:mm:ss，LocalDate -> yyyy-MM-dd，
 * 保证前后端交互的时间格式一致、可读。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(dateTimeFormatter),
                        new LocalDateSerializer(dateFormatter))
                .deserializers(new LocalDateTimeDeserializer(dateTimeFormatter),
                        new LocalDateDeserializer(dateFormatter));
    }
}
