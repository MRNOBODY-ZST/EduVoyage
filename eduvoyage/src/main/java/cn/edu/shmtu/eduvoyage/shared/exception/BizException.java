package cn.edu.shmtu.eduvoyage.shared.exception;

import lombok.Getter;

/**
 * Unchecked exception carrying a {@link BizErrorCode}. Thrown from any layer and
 * translated into a {@code Result} by {@code GlobalErrorWebExceptionHandler}.
 *
 * <p>Being a {@link RuntimeException} it propagates cleanly through Reactor
 * operators ({@code Mono.error} / {@code Flux.error}) without checked-exception
 * noise.</p>
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final transient BizErrorCode errorCode;

    public BizException(BizErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.code = errorCode.code();
    }

    public BizException(BizErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.code = errorCode.code();
    }

    public BizException(BizErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.code = errorCode.code();
    }

    public static BizException of(BizErrorCode errorCode) {
        return new BizException(errorCode);
    }

    public static BizException of(BizErrorCode errorCode, String message) {
        return new BizException(errorCode, message);
    }
}
