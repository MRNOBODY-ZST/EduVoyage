package cn.edu.shmtu.eduvoyage.shared.util;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Assigns a {@code traceId} to every request and:
 * <ul>
 *   <li>echoes it back in the {@code X-Trace-Id} response header,</li>
 *   <li>places it in the Reactor {@link Context} under {@link #TRACE_ID_KEY}
 *       so downstream operators can read it,</li>
 *   <li>writes it to SLF4J {@link MDC} during the reactive subscription so it
 *       appears in structured logs (see {@code logback-spring.xml}).</li>
 * </ul>
 *
 * <p>Runs very early in the filter chain so the id is available to security and
 * error handling.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdWebFilter implements WebFilter {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        String traceId = (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : incoming;

        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        exchange.getAttributes().put(TRACE_ID_KEY, traceId);

        return chain.filter(exchange)
                .contextWrite(Context.of(TRACE_ID_KEY, traceId))
                .doOnEach(signal -> {
                    if (signal.isOnNext() || signal.isOnError() || signal.isOnComplete()) {
                        // best-effort MDC population for logs emitted on this thread
                        MDC.put(TRACE_ID_KEY, traceId);
                    }
                })
                .doFinally(sig -> MDC.remove(TRACE_ID_KEY));
    }
}
