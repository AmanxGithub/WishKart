package com.wishkart.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wishkart.dto.ProductDTO;
import com.wishkart.entity.Product;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis configuration with type-safe templates and JSR310 support for Java 8+ date/time types.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Product> productRedisTemplate2(
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

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration config =
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(new StringRedisSerializer())
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(
                                                new GenericJackson2JsonRedisSerializer()
                                        )
                        );
        RedisCacheConfiguration productConfig =
                config.entryTtl(Duration.ofMinutes(2));

        RedisCacheConfiguration categoryConfig =
                config.entryTtl(Duration.ofHours(1));

        Map<String, RedisCacheConfiguration> cacheConfigurations =
                Map.of(
                        "products", productConfig,
                        "categories", categoryConfig
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
