package cn.edu.shmtu.eduvoyage.shared.security;

/**
 * Result of a successful authentication: a short-lived access token plus a
 * long-lived, revocable refresh token (whose jti is tracked in Redis).
 *
 * @param accessToken      JWT used as the {@code Authorization: Bearer} credential
 * @param refreshToken     JWT exchanged at {@code /api/auth/refresh} for a new pair
 * @param accessExpiresIn  access-token lifetime in seconds (for client scheduling)
 * @param tokenType        always {@code "Bearer"}
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        String tokenType
) {
    public static TokenPair bearer(String accessToken, String refreshToken, long accessExpiresIn) {
        return new TokenPair(accessToken, refreshToken, accessExpiresIn, "Bearer");
    }
}
