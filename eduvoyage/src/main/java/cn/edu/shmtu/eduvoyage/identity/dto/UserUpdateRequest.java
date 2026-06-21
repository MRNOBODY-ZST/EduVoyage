package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Admin-side user update. All fields optional; only non-null fields are applied
 * (partial update). {@code roleCodes != null} replaces the role set.
 */
@Schema(description = "更新用户请求（管理员）")
public record UserUpdateRequest(
        @Size(max = 64) String realName,
        @Email(message = "邮箱格式不正确") String email,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        Integer gender,
        String avatarUrl,
        @Schema(description = "状态 1正常0禁用2锁定") Integer status,
        Long classId,
        @Schema(description = "角色码集合；非空则整体替换") List<String> roleCodes
) {
}
