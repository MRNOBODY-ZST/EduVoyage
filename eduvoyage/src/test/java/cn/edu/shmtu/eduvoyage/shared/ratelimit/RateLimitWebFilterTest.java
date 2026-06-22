package cn.edu.shmtu.eduvoyage.shared.ratelimit;

import cn.edu.shmtu.eduvoyage.shared.config.EduVoyageProperties;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the filter-level handler lookup. The downstream chain
 * returns {@code Mono<Void>}, so successful completion must not be mistaken for
 * a missing handler and executed a second time.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitWebFilterTest {

    @Mock RequestMappingHandlerMapping handlerMapping;
    @Mock RedisRateLimiter rateLimiter;
    @Mock WebFilterChain chain;

    private RateLimitWebFilter filter;
    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        filter = new RateLimitWebFilter(handlerMapping, rateLimiter, properties(true));
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/courses").build());
    }

    @Test
    void handlerWithoutRateLimitRunsChainOnce() {
        AtomicInteger calls = new AtomicInteger();
        when(handlerMapping.getHandler(exchange)).thenReturn(Mono.just(new Object()));
        when(chain.filter(exchange)).thenAnswer(inv -> {
            calls.incrementAndGet();
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(calls.get()).isEqualTo(1);
        verify(rateLimiter, never()).tryAcquire(anyString(), eq(5L), eq(1L));
    }

    @Test
    void annotatedHandlerRunsLimiterThenChainOnceWhenAllowed() throws NoSuchMethodException {
        AtomicInteger calls = new AtomicInteger();
        HandlerMethod handler = new HandlerMethod(new SampleHandlers(), "limited");
        when(handlerMapping.getHandler(exchange)).thenReturn(Mono.just(handler));
        when(rateLimiter.tryAcquire(anyString(), eq(5L), eq(1L))).thenReturn(Mono.just(true));
        when(chain.filter(exchange)).thenAnswer(inv -> {
            calls.incrementAndGet();
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void annotatedHandlerRejectsWhenBucketIsEmpty() throws NoSuchMethodException {
        HandlerMethod handler = new HandlerMethod(new SampleHandlers(), "limited");
        when(handlerMapping.getHandler(exchange)).thenReturn(Mono.just(handler));
        when(rateLimiter.tryAcquire(anyString(), eq(5L), eq(1L))).thenReturn(Mono.just(false));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorSatisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(BizErrorCode.RATE_LIMITED))
                .verify();

        verify(chain, never()).filter(exchange);
    }

    private static EduVoyageProperties properties(boolean enabled) {
        return new EduVoyageProperties(
                null,
                new EduVoyageProperties.RateLimit(enabled, 100, 50),
                null,
                null);
    }

    static class SampleHandlers {
        @RateLimit(key = "sample", capacity = 5, refillPerSecond = 1)
        public Mono<Void> limited() {
            return Mono.empty();
        }
    }
}
