package cn.edu.shmtu.eduvoyage.shared.ratelimit;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Distributed token-bucket rate limiter backed by Redis. The bucket state
 * (tokens + last-refill timestamp) lives in a Redis hash and is updated
 * atomically by a Lua script, so concurrent requests across instances share one
 * limit without races.
 *
 * <p>Each call costs one token; {@link #tryAcquire} resolves to {@code true}
 * when a token was available. Buckets auto-expire after a period of inactivity
 * to avoid unbounded key growth.</p>
 */
@Component
public class RedisRateLimiter {

    /**
     * KEYS[1] = bucket key
     * ARGV[1] = capacity, ARGV[2] = refillPerSecond, ARGV[3] = nowMillis, ARGV[4] = requested
     * returns 1 if allowed else 0.
     */
    private static final String LUA = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested = tonumber(ARGV[4])

            local data = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(data[1])
            local ts = tonumber(data[2])
            if tokens == nil then
              tokens = capacity
              ts = now
            end

            local delta = math.max(0, now - ts) / 1000.0
            local refilled = math.min(capacity, tokens + delta * refill)

            local allowed = 0
            if refilled >= requested then
              allowed = 1
              refilled = refilled - requested
            end

            redis.call('HSET', key, 'tokens', refilled, 'ts', now)
            -- expire after the time it would take to fully refill, plus a margin
            local ttl = math.ceil(capacity / refill) + 1
            redis.call('EXPIRE', key, ttl)
            return allowed
            """;

    private static final RedisScript<Long> SCRIPT = RedisScript.of(LUA, Long.class);

    private final ReactiveStringRedisTemplate redis;

    public RedisRateLimiter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Attempts to consume one token from {@code bucketKey}'s bucket.
     *
     * @param bucketKey       unique bucket identity (e.g. {@code rl:login:1.2.3.4})
     * @param capacity        max burst (bucket size)
     * @param refillPerSecond steady-state tokens added per second
     * @return {@code true} if allowed, {@code false} if rate-limited
     */
    public Mono<Boolean> tryAcquire(String bucketKey, long capacity, long refillPerSecond) {
        long now = System.currentTimeMillis();
        return redis.execute(SCRIPT,
                        List.of(bucketKey),
                        List.of(String.valueOf(capacity),
                                String.valueOf(refillPerSecond),
                                String.valueOf(now),
                                "1"))
                .next()
                .map(allowed -> allowed != null && allowed == 1L)
                .defaultIfEmpty(true); // fail-open if Redis returns nothing
    }
}
