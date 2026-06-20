package cn.edu.shmtu.eduvoyage.shared.security;

import java.util.Set;

/**
 * Parsed, validated representation of a JWT's claims relevant to EduVoyage.
 *
 * @param userId   subject (user primary key)
 * @param username convenience claim
 * @param roles    role codes
 * @param perms    permission codes
 * @param jti      token id (used to revoke refresh tokens via Redis)
 * @param type     {@link JwtService#TYPE_ACCESS} or {@link JwtService#TYPE_REFRESH}
 */
public record JwtPayload(
        Long userId,
        String username,
        Set<String> roles,
        Set<String> perms,
        String jti,
        String type
) {
}
