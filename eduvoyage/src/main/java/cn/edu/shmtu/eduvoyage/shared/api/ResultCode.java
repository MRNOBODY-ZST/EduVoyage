package cn.edu.shmtu.eduvoyage.shared.api;

/**
 * Stable response codes shared across all modules. Kept separate from
 * {@code BizErrorCode} so success / generic transport codes live here while
 * domain-specific business errors carry richer enums.
 *
 * <p>Convention: {@code 0} = success; HTTP-aligned codes for generic
 * transport-level outcomes; module-specific 4xxxx/5xxxx codes are defined in
 * {@code BizErrorCode}.</p>
 */
public enum ResultCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "resource not found"),
    METHOD_NOT_ALLOWED(405, "method not allowed"),
    CONFLICT(409, "conflict"),
    PAYLOAD_TOO_LARGE(413, "payload too large"),
    TOO_MANY_REQUESTS(429, "too many requests"),
    INTERNAL_ERROR(500, "internal server error"),
    SERVICE_UNAVAILABLE(503, "service unavailable");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
