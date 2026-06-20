package cn.edu.shmtu.eduvoyage.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;

/**
 * Unified API envelope returned by every endpoint.
 *
 * @param <T> payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "统一返回体")
public record Result<T>(
        @Schema(description = "业务码，0 表示成功") int code,
        @Schema(description = "提示信息") String message,
        @Schema(description = "业务数据") T data,
        @Schema(description = "服务端时间戳(epoch millis)") long timestamp
) implements Serializable {

    private static <T> Result<T> of(int code, String message, T data) {
        return new Result<>(code, message, data, Instant.now().toEpochMilli());
    }

    public static <T> Result<T> success(T data) {
        return of(ResultCode.SUCCESS.code(), ResultCode.SUCCESS.message(), data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(String message, T data) {
        return of(ResultCode.SUCCESS.code(), message, data);
    }

    public static <T> Result<T> failure(int code, String message) {
        return of(code, message, null);
    }

    public static <T> Result<T> failure(ResultCode resultCode) {
        return of(resultCode.code(), resultCode.message(), null);
    }

    public boolean isSuccess() {
        return code == ResultCode.SUCCESS.code();
    }
}
