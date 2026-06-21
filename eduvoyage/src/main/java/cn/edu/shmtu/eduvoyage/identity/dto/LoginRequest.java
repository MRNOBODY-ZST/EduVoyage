package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request. The {@code captchaId}/{@code captchaCode} pair is verified
 * against the value stored in Redis under that id (see {@code CaptchaService}).
 */
@Schema(description = "登录请求")
public record LoginRequest(
        @Schema(description = "用户名", example = "admin")
        @NotBlank(message = "用户名不能为空")
        String username,

        @Schema(description = "密码", example = "Admin@123")
        @NotBlank(message = "密码不能为空")
        String password,

        @Schema(description = "验证码 id（来自 /api/auth/captcha）")
        String captchaId,

        @Schema(description = "验证码")
        String captchaCode
) {
}
