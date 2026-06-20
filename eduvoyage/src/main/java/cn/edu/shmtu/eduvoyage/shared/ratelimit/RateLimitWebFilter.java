package cn.edu.shmtu.eduvoyage.shared.ratelimit;

import cn.edu.shmtu.eduvoyage.shared.config.EduVoyageProperties;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Enforces {@link RateLimit} declared on annotated-controller handler methods.
 *
 * <p>Because {@code WebFilter}s execute before handler resolution, the matched
 * {@link HandlerMethod} is looked up via {@link RequestMappingHandlerMapping}
 * inside the filter; if the handler carries {@link RateLimit}, a token is
 * consumed from {@link RedisRateLimiter}. Exceeding the limit raises
 * {@code RATE_LIMITED} (HTTP 429).</p>
 *
 * <p>Global rate limiting can be disabled with {@code eduvoyage.ratelimit.enabled=false}.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class RateLimitWebFilter implements WebFilter {

    private final RequestMappingHandlerMapping handlerMapping;
    private final RedisRateLimiter rateLimiter;
    private final boolean enabled;

    public RateLimitWebFilter(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
                              RedisRateLimiter rateLimiter,
                              EduVoyageProperties properties) {
        this.handlerMapping = handlerMapping;
        this.rateLimiter = rateLimiter;
        this.enabled = properties.ratelimit().enabled();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }
        return handlerMapping.getHandler(exchange)
                .cast(Object.class)
                .flatMap(handler -> {
                    if (handler instanceof HandlerMethod hm) {
                        RateLimit rl = hm.getMethodAnnotation(RateLimit.class);
                        if (rl != null) {
                            return applyLimit(exchange, chain, rl);
                        }
                    }
                    return chain.filter(exchange);
                })
                // no matching handler (e.g. static/unmapped) -> let the chain proceed
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }

    private Mono<Void> applyLimit(ServerWebExchange exchange, WebFilterChain chain, RateLimit rl) {
        return resolveBucketKey(exchange, rl)
                .flatMap(bucketKey -> rateLimiter.tryAcquire(bucketKey, rl.capacity(), rl.refillPerSecond()))
                .flatMap(allowed -> allowed
                        ? chain.filter(exchange)
                        : Mono.error(new BizException(BizErrorCode.RATE_LIMITED)));
    }

    private Mono<String> resolveBucketKey(ServerWebExchange exchange, RateLimit rl) {
        String prefix = "eduvoyage:rl:" + rl.key() + ":";
        return switch (rl.keyType()) {
            case GLOBAL -> Mono.just(prefix + "global");
            case IP -> Mono.just(prefix + clientIp(exchange));
            case USER -> ReactiveSecurityContextHolder.getContext()
                    .map(ctx -> ctx.getAuthentication())
                    .filter(auth -> auth != null && auth.isAuthenticated()
                            && auth.getPrincipal() instanceof AuthUser)
                    .map(auth -> prefix + "u:" + ((AuthUser) auth.getPrincipal()).id())
                    .defaultIfEmpty(prefix + "ip:" + clientIp(exchange));
        };
    }

    private String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }
}
