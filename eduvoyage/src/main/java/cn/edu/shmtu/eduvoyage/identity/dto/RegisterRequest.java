package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Self-registration request. A user registers with either an email or a phone
 * (at least one), a username, and a verification code delivered out-of-band.
 */
@Schema(description = "注册请求")
public record RegisterRequest(
        @Schema(description = "用户名", example = "alice")
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 64, message = "用户名长度需在 3-64 之间")
        String username,

        @Schema(description = "密码（至少 8 位，含字母与数字）", example = "Passw0rd!")
        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$",
                message = "密码至少 8 位且需同时包含字母与数字")
        String password,

        @Schema(description = "真实姓名")
        @Size(max = 64, message = "姓名过长")
        String realName,

        @Schema(description = "邮箱（邮箱/手机至少填一项）")
        @Email(message = "邮箱格式不正确")
        String email,

        @Schema(description = "手机号（邮箱/手机至少填一项）")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Schema(description = "验证码 id")
        @NotBlank(message = "验证码 id 不能为空")
        String captchaId,

        @Schema(description = "验证码")
        @NotBlank(message = "验证码不能为空")
        String captchaCode
) {
}
