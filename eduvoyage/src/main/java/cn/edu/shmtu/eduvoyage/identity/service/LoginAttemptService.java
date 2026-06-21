package cn.edu.shmtu.eduvoyage.identity.service;

import cn.edu.shmtu.eduvoyage.shared.config.EduVoyageProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Tracks failed-login counts per username in Redis to enforce lockout after too
 * many consecutive failures.
 *
 * <p>Key layout: {@code eduvoyage:login:fail:{username} -> count} with a sliding
 * TTL equal to the configured lock duration. Once the count reaches the
 * threshold the account is considered locked until the key expires; a successful
 * login clears it.</p>
 */
@Service
public class LoginAttemptService {

    private static final String KEY_PREFIX = "eduvoyage:login:fail:";

    private final ReactiveStringRedisTemplate redis;
    private final int maxFailCount;
    private final Duration lockDuration;

    public LoginAttemptService(ReactiveStringRedisTemplate redis, EduVoyageProperties properties) {
        this.redis = redis;
        this.maxFailCount = properties.security().login().maxFailCount();
        this.lockDuration = properties.security().login().lockDuration();
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }

    /** True if the username currently has too many recent failures. */
    public Mono<Boolean> isLocked(String username) {
        return redis.opsForValue().get(key(username))
                .map(v -> parse(v) >= maxFailCount)
                .defaultIfEmpty(false);
    }

    /**
     * Records a failed attempt, (re)setting the TTL, and returns how many
     * attempts remain before lockout (0 once locked).
     */
    public Mono<Long> recordFailure(String username) {
        String k = key(username);
        return redis.opsForValue().increment(k)
                .flatMap(count -> redis.expire(k, lockDuration).thenReturn(count))
                .map(count -> Math.max(0, maxFailCount - count));
    }

    /** Clears the failure counter (call on successful login). */
    public Mono<Boolean> reset(String username) {
        return redis.delete(key(username)).map(n -> n > 0);
    }

    public int maxFailCount() {
        return maxFailCount;
    }

    private static long parse(String v) {
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
