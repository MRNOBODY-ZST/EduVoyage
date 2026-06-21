package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The authenticated user's own profile plus the role and permission codes the
 * frontend needs to render dynamic menus and apply {@code v-permission}.
 */
@Schema(description = "当前登录用户信息（含权限码）")
public record MeResponse(
        @Schema(description = "用户概要") UserResponse profile,
        @Schema(description = "角色码集合") List<String> roles,
        @Schema(description = "权限码集合") List<String> permissions
) {
}
