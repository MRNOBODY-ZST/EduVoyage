package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "创建角色请求")
public record RoleCreateRequest(
        @NotBlank(message = "角色码不能为空")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$",
                message = "角色码须为大写字母/数字/下划线，以字母开头")
        String code,

        @NotBlank(message = "角色名不能为空")
        @Size(max = 64)
        String name,

        @Size(max = 255) String description,

        @Schema(description = "权限码集合") List<String> permissionCodes
) {
}
