package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Token bundle returned by login / refresh. */
@Schema(description = "令牌响应")
public record TokenResponse(
        @Schema(description = "访问令牌（短时效）") String accessToken,
        @Schema(description = "刷新令牌（长时效，可吊销）") String refreshToken,
        @Schema(description = "令牌类型") String tokenType,
        @Schema(description = "访问令牌有效期（秒）") long expiresIn,
        @Schema(description = "登录用户概要") UserResponse user
) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
