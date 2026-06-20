package cn.edu.shmtu.eduvoyage.shared.security;

import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

/**
 * Convenience accessors for the authenticated {@link AuthUser} inside reactive
 * handlers. Handlers may also use Spring's {@code @AuthenticationPrincipal
 * AuthUser user} parameter; this helper is for service-layer code that has no
 * method parameter to bind.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Emits the current {@link AuthUser}, or errors with {@code UNAUTHENTICATED}. */
    public static Mono<AuthUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth != null && auth.isAuthenticated())
                .map(auth -> auth.getPrincipal())
                .filter(AuthUser.class::isInstance)
                .map(AuthUser.class::cast)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED)));
    }

    /** Emits just the current user's id, or errors with {@code UNAUTHENTICATED}. */
    public static Mono<Long> currentUserId() {
        return currentUser().map(AuthUser::id);
    }
}
