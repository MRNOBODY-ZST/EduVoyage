package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Partial role update; {@code permissionCodes != null} replaces the grant set. */
@Schema(description = "更新角色请求")
public record RoleUpdateRequest(
        @Size(max = 64) String name,
        @Size(max = 255) String description,
        @Schema(description = "权限码集合；非空则整体替换") List<String> permissionCodes
) {
}
