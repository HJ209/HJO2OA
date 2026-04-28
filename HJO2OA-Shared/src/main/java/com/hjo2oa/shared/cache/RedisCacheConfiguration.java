package com.hjo2oa.shared.cache;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisCacheConfiguration {

    @Bean
    @SuppressWarnings("unchecked")
    public RedisCacheOperations redisCacheOperations(RedisTemplate<?, ?> redisTemplate) {
        return new RedisCacheOperations((RedisTemplate<String, Object>) redisTemplate);
    }
}
