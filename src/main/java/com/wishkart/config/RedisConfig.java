package com.wishkart.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wishkart.entity.Product;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration with type-safe templates and JSR310 support for Java 8+ date/time types.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Product> productRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Product> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure Jackson serializer with JSR310 support
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Product> jackson = new Jackson2JsonRedisSerializer<>(Product.class);
        jackson.setObjectMapper(mapper);

        // Use String serializer for keys, Jackson for values
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson);

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson);

        template.afterPropertiesSet();

        return template;
    }
}
