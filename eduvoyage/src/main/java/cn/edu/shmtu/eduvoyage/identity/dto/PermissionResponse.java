package cn.edu.shmtu.eduvoyage.identity.dto;

import cn.edu.shmtu.eduvoyage.identity.domain.SysPermission;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "权限信息")
public record PermissionResponse(
        Long id, String code, String name, Integer type, Long parentId
) {
    public static PermissionResponse from(SysPermission p) {
        return new PermissionResponse(p.getId(), p.getCode(), p.getName(), p.getType(), p.getParentId());
    }
}
