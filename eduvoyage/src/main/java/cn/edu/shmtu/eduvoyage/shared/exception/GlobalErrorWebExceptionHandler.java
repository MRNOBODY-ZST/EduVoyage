package cn.edu.shmtu.eduvoyage.shared.exception;

import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Global WebFlux error handler. Implemented as an {@link org.springframework.web.server.WebExceptionHandler}
 * so it intercepts errors from BOTH annotated controllers and functional
 * router handlers, and runs after Spring's default chain has produced an
 * {@code ErrorAttributes} snapshot.
 *
 * <p>Every failure is rendered as the unified {@link Result} envelope with an
 * HTTP status that matches the business semantics, and the {@code traceId}
 * (populated by {@code TraceIdWebFilter}) is logged for correlation.</p>
 */
@Slf4j
@Component
@Order(-2) // before DefaultErrorWebExceptionHandler (-1) and ResponseStatusExceptionHandler (0)
public class GlobalErrorWebExceptionHandler implements org.springframework.web.server.WebExceptionHandler {

    private final JsonMapper jsonMapper;

    public GlobalErrorWebExceptionHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        Translated t = translate(ex);
        String path = exchange.getRequest().getPath().value();
        if (t.httpStatus.is5xxServerError()) {
            log.error("Unhandled error on {} -> code={} : {}", path, t.body.code(), ex.toString(), ex);
        } else {
            log.warn("Business error on {} -> code={} : {}", path, t.body.code(), t.body.message());
        }

        response.setStatusCode(t.httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = jsonMapper.writeValueAsBytes(t.body);
        } catch (Exception e) {
            bytes = ("{\"code\":10000,\"message\":\"serialization error\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private Translated translate(Throwable ex) {
        // Unwrap Reactor's wrapping where helpful
        if (ex instanceof BizException be) {
            BizErrorCode ec = be.getErrorCode();
            return new Translated(ec.httpStatus(), Result.failure(be.getCode(), be.getMessage()));
        }
        if (ex instanceof WebExchangeBindException bind) {
            String msg = bind.getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            return new Translated(HttpStatus.BAD_REQUEST,
                    Result.failure(BizErrorCode.PARAM_INVALID.code(),
                            msg.isBlank() ? BizErrorCode.PARAM_INVALID.message() : msg));
        }
        if (ex instanceof ServerWebInputException inputEx) {
            return new Translated(HttpStatus.BAD_REQUEST,
                    Result.failure(BizErrorCode.PARAM_INVALID.code(), inputEx.getReason()));
        }
        if (ex instanceof AuthenticationException) {
            return new Translated(HttpStatus.UNAUTHORIZED,
                    Result.failure(BizErrorCode.UNAUTHENTICATED.code(), BizErrorCode.UNAUTHENTICATED.message()));
        }
        if (ex instanceof AccessDeniedException) {
            return new Translated(HttpStatus.FORBIDDEN,
                    Result.failure(BizErrorCode.ACCESS_DENIED.code(), BizErrorCode.ACCESS_DENIED.message()));
        }
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            return new Translated(status,
                    Result.failure(status.value(), rse.getReason() != null ? rse.getReason() : status.getReasonPhrase()));
        }
        // fallback
        return new Translated(HttpStatus.INTERNAL_SERVER_ERROR,
                Result.failure(ResultCode.INTERNAL_ERROR.code(), ResultCode.INTERNAL_ERROR.message()));
    }

    private record Translated(HttpStatus httpStatus, Result<?> body) {
    }
}
