package cn.edu.shmtu.eduvoyage.shared.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a token-bucket rate limit for a handler. Enforcement is performed by
 * {@code RateLimitWebFilter}, which reads this annotation off the matched
 * handler method and applies {@link RedisRateLimiter} keyed by
 * {@link #keyType()} + {@link #key()}.
 *
 * <p>Example: limit login attempts to 5 burst / 1 per-second per client IP.</p>
 * <pre>{@code
 * @RateLimit(key = "login", capacity = 5, refillPerSecond = 1, keyType = KeyType.IP)
 * public Mono<...> login(...) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** Logical name of the limited action; part of the Redis bucket key. */
    String key();

    /** Maximum burst size (bucket capacity). */
    long capacity() default 100;

    /** Steady-state refill rate (tokens per second). */
    long refillPerSecond() default 50;

    /** What the bucket is scoped to. */
    KeyType keyType() default KeyType.IP;

    enum KeyType {
        /** One bucket per client IP. */
        IP,
        /** One bucket per authenticated user (falls back to IP if anonymous). */
        USER,
        /** A single global bucket for the action. */
        GLOBAL
    }
}
