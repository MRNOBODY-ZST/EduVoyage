package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Change password while logged in (requires the current password). */
@Schema(description = "修改密码")
public record ChangePasswordRequest(
        @NotBlank(message = "原密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$",
                message = "新密码至少 8 位且需同时包含字母与数字")
        String newPassword
) {
}
