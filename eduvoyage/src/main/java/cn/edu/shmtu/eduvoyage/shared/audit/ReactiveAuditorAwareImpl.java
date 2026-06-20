package cn.edu.shmtu.eduvoyage.shared.audit;

import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Supplies the current user id for {@code @CreatedBy}/{@code @LastModifiedBy}
 * auditing on R2DBC entities. Reads the authenticated {@link AuthUser} from the
 * reactive security context; falls back to {@code 0} (system) for anonymous /
 * unauthenticated writes (e.g. self-registration).
 */
@Component
public class ReactiveAuditorAwareImpl implements ReactiveAuditorAware<Long> {

    public static final long SYSTEM_AUDITOR = 0L;

    @Override
    public Mono<Long> getCurrentAuditor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth != null && auth.isAuthenticated())
                .map(auth -> auth.getPrincipal())
                .filter(AuthUser.class::isInstance)
                .map(principal -> ((AuthUser) principal).id())
                .defaultIfEmpty(SYSTEM_AUDITOR);
    }
}
