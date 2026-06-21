package cn.edu.shmtu.eduvoyage.identity.web;

import cn.edu.shmtu.eduvoyage.identity.dto.PermissionResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.RoleCreateRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.RoleResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.RoleUpdateRequest;
import cn.edu.shmtu.eduvoyage.identity.service.RoleService;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Role & permission administration. Reading roles/permissions only needs
 * {@code user:read}; mutating the RBAC model requires admin-level
 * {@code user:create}/{@code user:update}/{@code user:delete}.
 */
@Tag(name = "角色权限", description = "角色与权限管理")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "角色列表")
    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping
    public Mono<Result<List<RoleResponse>>> list() {
        return roleService.list().collectList().map(Result::success);
    }

    @Operation(summary = "全部权限列表（用于分配）")
    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping("/permissions")
    public Mono<Result<List<PermissionResponse>>> permissions() {
        return roleService.listAllPermissions().collectList().map(Result::success);
    }

    @Operation(summary = "角色详情")
    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping("/{id}")
    public Mono<Result<RoleResponse>> get(@PathVariable Long id) {
        return roleService.get(id).map(Result::success);
    }

    @Operation(summary = "创建角色")
    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping
    public Mono<Result<RoleResponse>> create(@Valid @RequestBody RoleCreateRequest req) {
        return roleService.create(req).map(Result::success);
    }

    @Operation(summary = "更新角色")
    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/{id}")
    public Mono<Result<RoleResponse>> update(@PathVariable Long id,
                                             @Valid @RequestBody RoleUpdateRequest req) {
        return roleService.update(id, req).map(Result::success);
    }

    @Operation(summary = "删除角色（逻辑删除，内置角色不可删）")
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id) {
        return roleService.delete(id).thenReturn(Result.<Void>success());
    }
}
