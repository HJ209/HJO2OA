package com.hjo2oa.shared.cache;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(RedisTemplate.class)
public class RedisCacheOperations {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheOperations(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(requireKey(key));
        if (value == null) {
            return Optional.empty();
        }
        if (!type.isInstance(value)) {
            throw new IllegalStateException("Redis value for key " + key + " is not a " + type.getName());
        }
        return Optional.of(type.cast(value));
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(requireKey(key), Objects.requireNonNull(value, "value must not be null"));
    }

    public void set(String key, Object value, Duration ttl) {
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            set(key, value);
            return;
        }
        redisTemplate.opsForValue().set(requireKey(key), Objects.requireNonNull(value, "value must not be null"), ttl);
    }

    public void delete(String key) {
        redisTemplate.delete(requireKey(key));
    }

    public void expire(String key, Duration ttl) {
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisTemplate.expire(requireKey(key), ttl);
    }

    private static String requireKey(String key) {
        Objects.requireNonNull(key, "key must not be null");
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return key;
    }
}
