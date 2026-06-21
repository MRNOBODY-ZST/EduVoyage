package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Captcha challenge. The {@code id} ties the client's later answer back to the
 * value cached in Redis. For dev/demo the {@code text} is returned directly; a
 * production deployment would instead return only an image data-URI.
 */
@Schema(description = "验证码")
public record CaptchaResponse(
        @Schema(description = "验证码 id（提交登录/注册时回传）") String id,
        @Schema(description = "验证码内容（dev 直接返回；prod 应改为图片）") String text,
        @Schema(description = "有效期（秒）") long ttlSeconds
) {
}
