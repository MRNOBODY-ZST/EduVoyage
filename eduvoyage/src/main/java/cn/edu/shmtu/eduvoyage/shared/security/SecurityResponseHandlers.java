package cn.edu.shmtu.eduvoyage.shared.security;

import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Renders authentication (401) and authorization (403) failures from the
 * security filter chain as the unified {@link Result} JSON envelope, matching
 * the format produced by {@code GlobalErrorWebExceptionHandler}.
 */
@Component
public class SecurityResponseHandlers
        implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public SecurityResponseHandlers(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return write(exchange, HttpStatus.UNAUTHORIZED,
                Result.failure(BizErrorCode.UNAUTHENTICATED.code(), BizErrorCode.UNAUTHENTICATED.message()));
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        return write(exchange, HttpStatus.FORBIDDEN,
                Result.failure(BizErrorCode.ACCESS_DENIED.code(), BizErrorCode.ACCESS_DENIED.message()));
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, Result<?> body) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = jsonMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = "{\"code\":10000,\"message\":\"error\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
