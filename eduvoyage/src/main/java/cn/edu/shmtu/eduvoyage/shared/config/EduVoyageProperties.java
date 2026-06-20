package cn.edu.shmtu.eduvoyage.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Strongly-typed binding for the {@code eduvoyage.*} configuration tree.
 * Centralising it keeps property keys discoverable and validated at startup.
 */
@ConfigurationProperties(prefix = "eduvoyage")
public record EduVoyageProperties(
        Security security,
        RateLimit ratelimit,
        Storage storage,
        Captcha captcha
) {

    public record Security(
            Jwt jwt,
            @DefaultValue({}) List<String> permitAll,
            Login login,
            @DefaultValue("multi") String concurrentStrategy
    ) {
    }

    public record Jwt(
            String secret,
            @DefaultValue("eduvoyage") String issuer,
            @DefaultValue("15m") Duration accessTokenTtl,
            @DefaultValue("7d") Duration refreshTokenTtl
    ) {
    }

    public record Login(
            @DefaultValue("5") int maxFailCount,
            @DefaultValue("15m") Duration lockDuration
    ) {
    }

    public record RateLimit(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("100") long defaultCapacity,
            @DefaultValue("50") long defaultRefillPerSecond
    ) {
    }

    public record Storage(
            Minio minio,
            Quota quota
    ) {
    }

    public record Minio(
            @DefaultValue("http://localhost:9000") String endpoint,
            @DefaultValue("minioadmin") String accessKey,
            @DefaultValue("minioadmin") String secretKey,
            @DefaultValue("eduvoyage") String bucket,
            @DefaultValue("1h") Duration presignExpiry
    ) {
    }

    public record Quota(
            @DefaultValue("2147483648") long studentBytes,
            @DefaultValue("10737418240") long teacherBytes,
            @DefaultValue("53687091200") long adminBytes
    ) {
    }

    public record Captcha(
            @DefaultValue("5m") Duration ttl,
            @DefaultValue("6") int length
    ) {
    }
}
