package com.fastbills.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule for Java 8 date/time types
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Serialize BigDecimal strictly as String to protect precision on JavaScript clients
        SimpleModule bigDecimalModule = new SimpleModule("BigDecimalModule");
        bigDecimalModule.addSerializer(BigDecimal.class, new ToStringSerializer());
        mapper.registerModule(bigDecimalModule);

        return mapper;
    }
}
