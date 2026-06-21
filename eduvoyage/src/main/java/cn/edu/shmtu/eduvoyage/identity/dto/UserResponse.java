package cn.edu.shmtu.eduvoyage.identity.dto;

import cn.edu.shmtu.eduvoyage.identity.domain.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbound user view. Never carries the password hash. {@code roles} is included
 * so list/detail screens can show role badges without an extra call.
 */
@Schema(description = "用户信息")
public record UserResponse(
        @Schema(description = "用户 id") Long id,
        @Schema(description = "用户名") String username,
        @Schema(description = "真实姓名") String realName,
        @Schema(description = "邮箱") String email,
        @Schema(description = "手机号") String phone,
        @Schema(description = "头像 URL") String avatarUrl,
        @Schema(description = "性别 0未知1男2女") Integer gender,
        @Schema(description = "状态 1正常0禁用2锁定") Integer status,
        @Schema(description = "班级 id") Long classId,
        @Schema(description = "最近登录时间") LocalDateTime lastLoginAt,
        @Schema(description = "角色码") List<String> roles
) {

    /** Maps an entity to a view, attaching the given role codes. */
    public static UserResponse from(SysUser u, List<String> roles) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getRealName(), u.getEmail(), u.getPhone(),
                u.getAvatarUrl(), u.getGender(), u.getStatus(), u.getClassId(),
                u.getLastLoginAt(), roles);
    }

    public static UserResponse from(SysUser u) {
        return from(u, List.of());
    }
}
