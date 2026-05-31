package com.wwun.acme.catalog.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConf = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );

        Map<String, RedisCacheConfiguration> config = new HashMap<>();
        config.put("catalogProductsByIds", defaultConf.entryTtl(Duration.ofSeconds(30)));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConf)
            .withInitialCacheConfigurations(config)
            .build();
    }

    @Bean("catalogProductIdsKeyGenerator")
    public KeyGenerator catalogProductIdsKeyGenerator() {
        return (target, method, params) -> {
            if (params.length == 0 || !(params[0] instanceof Iterable<?> productIds)) {
                return SimpleKeyGenerator.generateKey(params);
            }

            return productIds.iterator().hasNext()
                ? streamProductIds(productIds)
                : SimpleKeyGenerator.generateKey(params);
        };
    }

    private String streamProductIds(Iterable<?> productIds) {
        return java.util.stream.StreamSupport.stream(productIds.spliterator(), false)
            .filter(Objects::nonNull)
            .map(Object::toString)
            .distinct()
            .sorted()
            .collect(Collectors.joining(","));
    }
}
