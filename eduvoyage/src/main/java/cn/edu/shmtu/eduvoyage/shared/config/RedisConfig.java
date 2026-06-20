package cn.edu.shmtu.eduvoyage.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Reactive Redis templates. {@link ReactiveStringRedisTemplate} is used for
 * tokens, counters, rate-limit buckets and locks; a JSON-valued template is
 * provided for caching small DTOs.
 *
 * <p>Uses {@link GenericJacksonJsonRedisSerializer} (Jackson 3 based, the
 * Spring Data Redis 4 replacement for the deprecated
 * {@code Jackson2JsonRedisSerializer}).</p>
 */
@Configuration(proxyBeanMethods = false)
public class RedisConfig {

    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveStringRedisTemplate(factory);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveJsonRedisTemplate(ReactiveRedisConnectionFactory factory) {
        GenericJacksonJsonRedisSerializer valueSerializer = GenericJacksonJsonRedisSerializer.builder().build();
        StringRedisSerializer keySerializer = StringRedisSerializer.UTF_8;

        RedisSerializationContext<String, Object> context = RedisSerializationContext
                .<String, Object>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
