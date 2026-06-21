package cn.edu.shmtu.eduvoyage.identity.web;

import cn.edu.shmtu.eduvoyage.identity.dto.CaptchaResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.ForgotPasswordRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.LoginRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.MeResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.RefreshRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.RegisterRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.TokenResponse;
import cn.edu.shmtu.eduvoyage.identity.service.AuthService;
import cn.edu.shmtu.eduvoyage.identity.service.CaptchaService;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Authentication endpoints. All paths here are on the {@code permit-all}
 * allowlist except {@code /me}, which requires a valid access token.
 */
@Tag(name = "认证", description = "登录、注册、令牌刷新、登出、找回密码")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Mono<Result<CaptchaResponse>> captcha() {
        return captchaService.generate().map(Result::success);
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Mono<Result<TokenResponse>> login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req).map(Result::success);
    }

    @Operation(summary = "注册（默认学生角色）")
    @PostMapping("/register")
    public Mono<Result<TokenResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req).map(Result::success);
    }

    @Operation(summary = "刷新令牌（旋转刷新令牌）")
    @PostMapping("/refresh")
    public Mono<Result<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken()).map(Result::success);
    }

    @Operation(summary = "登出（吊销刷新令牌）")
    @PostMapping("/logout")
    public Mono<Result<Void>> logout(@RequestBody(required = false) RefreshRequest req) {
        String token = req == null ? null : req.refreshToken();
        return authService.logout(token).thenReturn(Result.<Void>success());
    }

    @Operation(summary = "找回密码")
    @PostMapping("/password/forgot")
    public Mono<Result<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return authService.forgotPassword(req).thenReturn(Result.<Void>success());
    }

    @Operation(summary = "获取当前登录用户信息（含权限码）")
    @GetMapping("/me")
    public Mono<Result<MeResponse>> me(@AuthenticationPrincipal AuthUser user) {
        return authService.me(user.id()).map(Result::success);
    }
}
