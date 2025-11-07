package com.wwun.acme.product.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class RedisConfig {

    // cache manager will be implemented
    // @Bean
    // public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
    //     RedisTemplate<String, Object> template = new RedisTemplate<>();
    //     template.setConnectionFactory(redisConnectionFactory);
    //     template.setKeySerializer(new StringRedisSerializer());
    //     template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    //     template.setHashKeySerializer(new StringRedisSerializer());
    //     template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    //     return template;
    // }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConf = RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues();

        Map<String, RedisCacheConfiguration> config = new HashMap<>();
        config.put("productById", defaultConf.entryTtl(Duration.ofSeconds(300))); // 5 min
        config.put("productsAll", defaultConf.entryTtl(Duration.ofSeconds(60)));  // 1 min

        return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(defaultConf).withInitialCacheConfigurations(config).build();
    }

}
