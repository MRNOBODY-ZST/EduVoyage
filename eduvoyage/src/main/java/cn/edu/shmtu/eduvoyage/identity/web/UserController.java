package cn.edu.shmtu.eduvoyage.identity.web;

import cn.edu.shmtu.eduvoyage.identity.dto.ChangePasswordRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.ProfileUpdateRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.UserCreateRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.UserResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.UserUpdateRequest;
import cn.edu.shmtu.eduvoyage.identity.service.UserService;
import cn.edu.shmtu.eduvoyage.shared.api.PageResult;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * User management. Admin-only collection operations are guarded by
 * {@code @PreAuthorize} permission checks; the {@code /me/**} self-service
 * endpoints only require authentication.
 */
@Tag(name = "用户管理", description = "用户增删改查、个人资料、修改密码")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页查询用户（管理员）")
    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping
    public Mono<Result<PageResult<UserResponse>>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return userService.page(keyword, status, classId, pageNo, pageSize).map(Result::success);
    }

    @Operation(summary = "查看用户详情（管理员）")
    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping("/{id}")
    public Mono<Result<UserResponse>> get(@PathVariable Long id) {
        return userService.get(id).map(Result::success);
    }

    @Operation(summary = "创建用户（管理员）")
    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping
    public Mono<Result<UserResponse>> create(@Valid @RequestBody UserCreateRequest req) {
        return userService.create(req).map(Result::success);
    }

    @Operation(summary = "更新用户（管理员）")
    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/{id}")
    public Mono<Result<UserResponse>> update(@PathVariable Long id,
                                             @Valid @RequestBody UserUpdateRequest req) {
        return userService.update(id, req).map(Result::success);
    }

    @Operation(summary = "删除用户（管理员，逻辑删除）")
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id,
                                     @AuthenticationPrincipal AuthUser operator) {
        return userService.delete(id, operator.id()).thenReturn(Result.<Void>success());
    }

    // ----------------------------------------------------------- self ops

    @Operation(summary = "更新个人资料")
    @PutMapping("/me/profile")
    public Mono<Result<UserResponse>> updateProfile(@AuthenticationPrincipal AuthUser user,
                                                    @Valid @RequestBody ProfileUpdateRequest req) {
        return userService.updateProfile(user.id(), req).map(Result::success);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public Mono<Result<Void>> changePassword(@AuthenticationPrincipal AuthUser user,
                                             @Valid @RequestBody ChangePasswordRequest req) {
        return userService.changePassword(user.id(), req).thenReturn(Result.<Void>success());
    }
}
