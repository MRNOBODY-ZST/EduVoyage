package cn.edu.shmtu.eduvoyage.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * Unified pagination envelope. Carried as the {@code data} of a {@link Result}.
 *
 * @param <T> row type
 */
@Schema(description = "统一分页结果")
public record PageResult<T>(
        @Schema(description = "当前页数据") List<T> records,
        @Schema(description = "总记录数") long total,
        @Schema(description = "页码，从 1 开始") int pageNo,
        @Schema(description = "每页大小") int pageSize,
        @Schema(description = "总页数") int totalPages
) implements Serializable {

    public static <T> PageResult<T> of(List<T> records, long total, int pageNo, int pageSize) {
        int safeSize = pageSize <= 0 ? 1 : pageSize;
        int totalPages = (int) ((total + safeSize - 1) / safeSize);
        return new PageResult<>(records, total, pageNo, pageSize, totalPages);
    }

    public static <T> PageResult<T> empty(int pageNo, int pageSize) {
        return new PageResult<>(List.of(), 0L, pageNo, pageSize, 0);
    }
}
