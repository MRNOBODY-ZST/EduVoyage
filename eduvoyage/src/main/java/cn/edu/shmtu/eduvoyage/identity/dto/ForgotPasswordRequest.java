package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Reset a forgotten password using a verification code sent to the account's
 * email/phone. The {@code account} is the username, email, or phone.
 */
@Schema(description = "找回密码")
public record ForgotPasswordRequest(
        @NotBlank(message = "账号不能为空")
        String account,

        @NotBlank(message = "验证码 id 不能为空")
        String captchaId,

        @NotBlank(message = "验证码不能为空")
        String captchaCode,

        @NotBlank(message = "新密码不能为空")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$",
                message = "新密码至少 8 位且需同时包含字母与数字")
        String newPassword
) {
}
