package cn.edu.shmtu.eduvoyage.shared.security;

import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts a Bearer access token from the {@code Authorization} header, verifies
 * it via {@link JwtService}, and converts it into a {@link JwtAuthenticationToken}.
 *
 * <p>Only access tokens are accepted here; presenting a refresh token to a
 * protected resource is rejected with {@code TOKEN_INVALID}. Requests without a
 * Bearer header resolve to an empty {@link Mono} so the security chain can apply
 * its anonymous / 401 handling.</p>
 */
@Component
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtServerAuthenticationConverter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Mono.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return Mono.fromCallable(() -> jwtService.parse(token))
                .handle((payload, sink) -> {
                    if (!JwtService.TYPE_ACCESS.equals(payload.type())) {
                        sink.error(new BizException(BizErrorCode.TOKEN_INVALID, "需要访问令牌"));
                        return;
                    }
                    AuthUser user = new AuthUser(
                            payload.userId(), payload.username(), payload.roles(), payload.perms());
                    sink.next(new JwtAuthenticationToken(user));
                });
    }
}
