package cn.edu.shmtu.eduvoyage.identity.service;

import cn.edu.shmtu.eduvoyage.identity.dto.CaptchaResponse;
import cn.edu.shmtu.eduvoyage.shared.config.EduVoyageProperties;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Issues and verifies short-lived captcha challenges stored in Redis.
 *
 * <p>Key layout: {@code eduvoyage:captcha:{id} -> code} with the configured TTL.
 * Verification is single-use: a correct match deletes the key so a code cannot
 * be replayed. For dev the plain code is returned in the response; production
 * should swap {@link #generate()} to emit an image and keep the code server-side
 * only.</p>
 */
@Service
public class CaptchaService {

    private static final String KEY_PREFIX = "eduvoyage:captcha:";
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final ReactiveStringRedisTemplate redis;
    private final IdGenerator idGenerator;
    private final SecureRandom random = new SecureRandom();
    private final Duration ttl;
    private final int length;

    public CaptchaService(ReactiveStringRedisTemplate redis,
                          IdGenerator idGenerator,
                          EduVoyageProperties properties) {
        this.redis = redis;
        this.idGenerator = idGenerator;
        this.ttl = properties.captcha().ttl();
        this.length = properties.captcha().length();
    }

    /** Generates a captcha, stores it, and returns the id + (dev) text. */
    public Mono<CaptchaResponse> generate() {
        String id = idGenerator.nextIdString();
        String code = randomCode();
        return redis.opsForValue()
                .set(KEY_PREFIX + id, code, ttl)
                .thenReturn(new CaptchaResponse(id, code, ttl.toSeconds()));
    }

    /**
     * Verifies a submitted code against the stored value and consumes it.
     *
     * @return {@code true} if the code matched (case-insensitive)
     */
    public Mono<Boolean> verify(String id, String code) {
        if (id == null || id.isBlank() || code == null || code.isBlank()) {
            return Mono.just(false);
        }
        String key = KEY_PREFIX + id;
        return redis.opsForValue().get(key)
                .flatMap(stored -> redis.delete(key)
                        .thenReturn(stored.equalsIgnoreCase(code.trim())))
                .defaultIfEmpty(false);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
