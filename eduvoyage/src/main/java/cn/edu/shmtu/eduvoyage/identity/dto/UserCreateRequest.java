package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Admin-side user creation (assigns roles directly). */
@Schema(description = "创建用户请求（管理员）")
public record UserCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 64)
        String username,

        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$",
                message = "密码至少 8 位且需同时包含字母与数字")
        String password,

        @Size(max = 64) String realName,

        @Email(message = "邮箱格式不正确") String email,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,

        @Schema(description = "性别 0未知1男2女") Integer gender,

        @Schema(description = "班级 id（学生）") Long classId,

        @Schema(description = "角色码集合，如 [\"TEACHER\"]")
        @NotEmpty(message = "至少分配一个角色")
        List<String> roleCodes
) {
}
