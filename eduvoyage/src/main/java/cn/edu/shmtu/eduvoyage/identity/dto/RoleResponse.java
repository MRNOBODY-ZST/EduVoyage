package cn.edu.shmtu.eduvoyage.identity.dto;

import cn.edu.shmtu.eduvoyage.identity.domain.SysRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "角色信息")
public record RoleResponse(
        @Schema(description = "角色 id") Long id,
        @Schema(description = "角色码") String code,
        @Schema(description = "角色名") String name,
        @Schema(description = "描述") String description,
        @Schema(description = "权限码集合") List<String> permissions
) {
    public static RoleResponse from(SysRole r, List<String> permissions) {
        return new RoleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(), permissions);
    }
}
