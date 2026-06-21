package cn.edu.shmtu.eduvoyage.course.repository;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dynamic, paginated course-catalog search. The filter set (keyword, teacher,
 * status, visibility) is optional and composed at runtime, so it's expressed with
 * {@link DatabaseClient} rather than derived {@code @Query} methods.
 */
@Repository
public class CourseQueryRepository {

    private final DatabaseClient db;

    public CourseQueryRepository(DatabaseClient db) {
        this.db = db;
    }

    public Flux<Course> search(String keyword, Long teacherId, Integer status, Integer visibility,
                               int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM course WHERE deleted = 0");
        appendFilters(sql, keyword, teacherId, status, visibility);
        sql.append(" ORDER BY id DESC LIMIT :limit OFFSET :offset");

        GenericExecuteSpec spec = db.sql(sql.toString());
        spec = bindFilters(spec, keyword, teacherId, status, visibility);
        spec = spec.bind("limit", limit).bind("offset", offset);
        return spec.map(CourseQueryRepository::mapCourse).all();
    }

    public Mono<Long> count(String keyword, Long teacherId, Integer status, Integer visibility) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM course WHERE deleted = 0");
        appendFilters(sql, keyword, teacherId, status, visibility);

        GenericExecuteSpec spec = db.sql(sql.toString());
        spec = bindFilters(spec, keyword, teacherId, status, visibility);
        return spec.map((row, meta) -> row.get("cnt", Long.class)).one().defaultIfEmpty(0L);
    }

    private static void appendFilters(StringBuilder sql, String keyword, Long teacherId,
                                      Integer status, Integer visibility) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (title LIKE :kw OR intro LIKE :kw)");
        }
        if (teacherId != null) {
            sql.append(" AND teacher_id = :teacherId");
        }
        if (status != null) {
            sql.append(" AND status = :status");
        }
        if (visibility != null) {
            sql.append(" AND visibility = :visibility");
        }
    }

    private static GenericExecuteSpec bindFilters(GenericExecuteSpec spec, String keyword, Long teacherId,
                                                  Integer status, Integer visibility) {
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.bind("kw", "%" + keyword.trim() + "%");
        }
        if (teacherId != null) {
            spec = spec.bind("teacherId", teacherId);
        }
        if (status != null) {
            spec = spec.bind("status", status);
        }
        if (visibility != null) {
            spec = spec.bind("visibility", visibility);
        }
        return spec;
    }

    private static Course mapCourse(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata meta) {
        return Course.builder()
                .id(row.get("id", Long.class))
                .title(row.get("title", String.class))
                .coverUrl(row.get("cover_url", String.class))
                .intro(row.get("intro", String.class))
                .credit(row.get("credit", BigDecimal.class))
                .teacherId(row.get("teacher_id", Long.class))
                .visibility(row.get("visibility", Integer.class))
                .status(row.get("status", Integer.class))
                .startDate(row.get("start_date", LocalDate.class))
                .endDate(row.get("end_date", LocalDate.class))
                .createdBy(row.get("created_by", Long.class))
                .createdAt(row.get("created_at", LocalDateTime.class))
                .updatedAt(row.get("updated_at", LocalDateTime.class))
                .deleted(row.get("deleted", Integer.class))
                .build();
    }
}
