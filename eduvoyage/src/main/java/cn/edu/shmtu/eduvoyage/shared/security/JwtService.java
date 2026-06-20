package cn.edu.shmtu.eduvoyage.shared.security;

import cn.edu.shmtu.eduvoyage.shared.config.EduVoyageProperties;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stateless JWT minting and verification for the dual-token scheme.
 *
 * <ul>
 *   <li><b>Access token</b> ({@value #TYPE_ACCESS}) — short-lived (15m),
 *       carries roles + permissions so authorization needs no DB hit.</li>
 *   <li><b>Refresh token</b> ({@value #TYPE_REFRESH}) — long-lived (7d), carries
 *       a {@code jti} that {@code RefreshTokenStore} tracks in Redis so it can be
 *       revoked (logout / rotation / forced logout).</li>
 * </ul>
 *
 * <p>Signing uses HMAC-SHA256 over a configured secret (≥32 bytes; override in
 * prod via {@code EDUVOYAGE_JWT_SECRET}).</p>
 */
@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMS = "perms";
    private static final String CLAIM_TYPE = "typ";

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtService(EduVoyageProperties properties) {
        EduVoyageProperties.Jwt jwt = properties.security().jwt();
        byte[] secretBytes = jwt.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("eduvoyage.security.jwt.secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = jwt.issuer();
        this.accessTtlSeconds = jwt.accessTokenTtl().toSeconds();
        this.refreshTtlSeconds = jwt.refreshTokenTtl().toSeconds();
    }

    /** Mints an access token embedding roles + permissions for the given user. */
    public String generateAccessToken(AuthUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.id()))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USERNAME, user.username())
                .claim(CLAIM_ROLES, user.roles())
                .claim(CLAIM_PERMS, user.permissions())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Mints a refresh token. The returned {@code jti} must be persisted in Redis
     * by the caller so the token can later be validated/revoked.
     */
    public RefreshToken generateRefreshToken(Long userId, String username) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .id(jti)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .signWith(signingKey)
                .compact();
        return new RefreshToken(token, jti, refreshTtlSeconds);
    }

    /**
     * Parses and verifies a token's signature and expiry, returning its payload.
     *
     * @throws BizException {@code TOKEN_EXPIRED} or {@code TOKEN_INVALID} on failure
     */
    public JwtPayload parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            Claims c = jws.getPayload();
            return new JwtPayload(
                    Long.valueOf(c.getSubject()),
                    c.get(CLAIM_USERNAME, String.class),
                    toStringSet(c.get(CLAIM_ROLES)),
                    toStringSet(c.get(CLAIM_PERMS)),
                    c.getId(),
                    c.get(CLAIM_TYPE, String.class));
        } catch (ExpiredJwtException e) {
            throw new BizException(BizErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(BizErrorCode.TOKEN_INVALID);
        }
    }

    public long accessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long refreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    private static Set<String> toStringSet(Object claim) {
        if (claim instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        if (claim instanceof Set<?> set) {
            return set.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        return Set.of();
    }

    /** Carrier for a freshly-minted refresh token + its jti and ttl. */
    public record RefreshToken(String token, String jti, long ttlSeconds) {
    }
}
