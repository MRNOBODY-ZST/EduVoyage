package cn.edu.shmtu.eduvoyage.identity.service;

import cn.edu.shmtu.eduvoyage.identity.domain.SysPermission;
import cn.edu.shmtu.eduvoyage.identity.domain.SysRole;
import cn.edu.shmtu.eduvoyage.identity.dto.PermissionResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.RoleCreateRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.RoleResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.RoleUpdateRequest;
import cn.edu.shmtu.eduvoyage.identity.repository.RbacRepository;
import cn.edu.shmtu.eduvoyage.identity.repository.SysPermissionRepository;
import cn.edu.shmtu.eduvoyage.identity.repository.SysRoleRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Role and permission administration. A role's permission grants are stored in
 * the {@code sys_role_permission} join table and replaced atomically via
 * {@link RbacRepository}.
 */
@Service
public class RoleService {

    private final SysRoleRepository roleRepository;
    private final SysPermissionRepository permissionRepository;
    private final RbacRepository rbacRepository;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public RoleService(SysRoleRepository roleRepository,
                       SysPermissionRepository permissionRepository,
                       RbacRepository rbacRepository,
                       R2dbcEntityTemplate entityTemplate,
                       IdGenerator idGenerator) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rbacRepository = rbacRepository;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    public Flux<RoleResponse> list() {
        return roleRepository.findAllActive()
                .flatMap(role -> rbacRepository.findPermissionCodesByRoleId(role.getId())
                        .collectList()
                        .map(perms -> RoleResponse.from(role, perms)));
    }

    public Mono<RoleResponse> get(Long id) {
        return roleRepository.findById(id)
                .filter(r -> r.getDeleted() == null || r.getDeleted() == 0)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "角色不存在")))
                .flatMap(role -> rbacRepository.findPermissionCodesByRoleId(role.getId())
                        .collectList()
                        .map(perms -> RoleResponse.from(role, perms)));
    }

    public Flux<PermissionResponse> listAllPermissions() {
        return permissionRepository.findAllActive().map(PermissionResponse::from);
    }

    @Transactional
    public Mono<RoleResponse> create(RoleCreateRequest req) {
        return roleRepository.countByCode(req.code())
                .flatMap(n -> n > 0
                        ? Mono.error(new BizException(BizErrorCode.DATA_CONFLICT, "角色码已存在"))
                        : Mono.empty())
                .then(resolvePermissionIds(req.permissionCodes()))
                .flatMap(permIds -> {
                    SysRole role = SysRole.builder()
                            .id(idGenerator.nextId())
                            .code(req.code())
                            .name(req.name())
                            .description(req.description())
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(SysRole.class).using(role)
                            .flatMap(saved -> rbacRepository.replaceRolePermissions(saved.getId(), permIds)
                                    .thenReturn(saved));
                })
                .flatMap(role -> get(role.getId()));
    }

    @Transactional
    public Mono<RoleResponse> update(Long id, RoleUpdateRequest req) {
        return roleRepository.findById(id)
                .filter(r -> r.getDeleted() == null || r.getDeleted() == 0)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "角色不存在")))
                .flatMap(role -> {
                    if (req.name() != null) {
                        role.setName(req.name());
                    }
                    if (req.description() != null) {
                        role.setDescription(req.description());
                    }
                    Mono<Void> permUpdate = req.permissionCodes() == null ? Mono.empty()
                            : resolvePermissionIds(req.permissionCodes())
                            .flatMap(ids -> rbacRepository.replaceRolePermissions(id, ids));
                    return roleRepository.save(role).then(permUpdate);
                })
                .then(get(id));
    }

    @Transactional
    public Mono<Void> delete(Long id) {
        return roleRepository.findById(id)
                .filter(r -> r.getDeleted() == null || r.getDeleted() == 0)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "角色不存在")))
                .flatMap(role -> {
                    if (SysRole.ADMIN.equals(role.getCode())
                            || SysRole.TEACHER.equals(role.getCode())
                            || SysRole.STUDENT.equals(role.getCode())) {
                        return Mono.error(new BizException(
                                BizErrorCode.OPERATION_NOT_ALLOWED, "内置角色不可删除"));
                    }
                    role.setDeleted(1);
                    return roleRepository.save(role);
                })
                .then(rbacRepository.deleteRolePermissions(id));
    }

    /** Maps permission codes to ids, failing if any code is unknown. */
    private Mono<List<Long>> resolvePermissionIds(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Mono.just(List.of());
        }
        return permissionRepository.findAllActive()
                .collectMap(SysPermission::getCode, Function.identity())
                .flatMap(byCode -> {
                    Map<String, SysPermission> map = byCode;
                    for (String code : codes) {
                        if (!map.containsKey(code)) {
                            return Mono.error(new BizException(
                                    BizErrorCode.PARAM_INVALID, "权限码不存在: " + code));
                        }
                    }
                    List<Long> ids = codes.stream()
                            .map(c -> map.get(c).getId())
                            .collect(Collectors.toList());
                    return Mono.just(ids);
                });
    }
}
