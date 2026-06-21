package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Self-service profile update by the logged-in user. */
@Schema(description = "更新个人资料")
public record ProfileUpdateRequest(
        @Size(max = 64) String realName,
        @Email(message = "邮箱格式不正确") String email,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        Integer gender,
        String avatarUrl
) {
}
