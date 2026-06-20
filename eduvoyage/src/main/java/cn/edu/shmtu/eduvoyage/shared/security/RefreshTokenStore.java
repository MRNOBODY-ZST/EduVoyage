package cn.edu.shmtu.eduvoyage.shared.security;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis-backed registry of valid refresh-token jtis, enabling revocation
 * (logout, rotation, forced logout) of otherwise-stateless JWTs.
 *
 * <p>Key layout: {@code eduvoyage:refresh:{userId}:{jti} -> "1"} with the
 * token's TTL. A refresh token is only honoured if its jti key is still
 * present; logout deletes it.</p>
 */
@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "eduvoyage:refresh:";

    private final ReactiveStringRedisTemplate redis;

    public RefreshTokenStore(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(Long userId, String jti) {
        return KEY_PREFIX + userId + ":" + jti;
    }

    /** Stores a freshly-issued refresh-token jti with its TTL. */
    public Mono<Boolean> store(Long userId, String jti, long ttlSeconds) {
        return redis.opsForValue().set(key(userId, jti), "1", Duration.ofSeconds(ttlSeconds));
    }

    /** True if the jti is still valid (present and not expired/revoked). */
    public Mono<Boolean> isValid(Long userId, String jti) {
        return redis.hasKey(key(userId, jti));
    }

    /** Revokes a single refresh token (e.g. on rotation or logout). */
    public Mono<Boolean> revoke(Long userId, String jti) {
        return redis.delete(key(userId, jti)).map(n -> n > 0);
    }

    /** Revokes every refresh token for a user (force logout / password change). */
    public Mono<Long> revokeAll(Long userId) {
        return redis.keys(KEY_PREFIX + userId + ":*")
                .collectList()
                .flatMap(keys -> keys.isEmpty() ? Mono.just(0L) : redis.delete(keys.toArray(String[]::new)));
    }
}
