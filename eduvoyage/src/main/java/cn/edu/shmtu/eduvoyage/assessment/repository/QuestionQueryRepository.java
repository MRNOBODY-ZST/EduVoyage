package cn.edu.shmtu.eduvoyage.assessment.repository;

import cn.edu.shmtu.eduvoyage.assessment.domain.Question;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Dynamic, paginated question-bank search. The filter set (course, keyword on
 * stem, type, difficulty, knowledge node) is optional and composed at runtime,
 * so it's expressed with {@link DatabaseClient} rather than derived queries.
 */
@Repository
public class QuestionQueryRepository {

    private final DatabaseClient db;

    public QuestionQueryRepository(DatabaseClient db) {
        this.db = db;
    }

    public Flux<Question> search(Long courseId, String keyword, Integer type, Integer difficulty,
                                 Long nodeId, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM question WHERE deleted = 0");
        appendFilters(sql, courseId, keyword, type, difficulty, nodeId);
        sql.append(" ORDER BY id DESC LIMIT :limit OFFSET :offset");

        GenericExecuteSpec spec = db.sql(sql.toString());
        spec = bindFilters(spec, courseId, keyword, type, difficulty, nodeId);
        spec = spec.bind("limit", limit).bind("offset", offset);
        return spec.map(QuestionQueryRepository::mapQuestion).all();
    }

    public Mono<Long> count(Long courseId, String keyword, Integer type, Integer difficulty, Long nodeId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM question WHERE deleted = 0");
        appendFilters(sql, courseId, keyword, type, difficulty, nodeId);

        GenericExecuteSpec spec = db.sql(sql.toString());
        spec = bindFilters(spec, courseId, keyword, type, difficulty, nodeId);
        return spec.map((row, meta) -> row.get("cnt", Long.class)).one().defaultIfEmpty(0L);
    }

    private static void appendFilters(StringBuilder sql, Long courseId, String keyword,
                                      Integer type, Integer difficulty, Long nodeId) {
        if (courseId != null) {
            sql.append(" AND course_id = :courseId");
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND stem LIKE :kw");
        }
        if (type != null) {
            sql.append(" AND type = :type");
        }
        if (difficulty != null) {
            sql.append(" AND difficulty = :difficulty");
        }
        if (nodeId != null) {
            sql.append(" AND node_id = :nodeId");
        }
    }

    private static GenericExecuteSpec bindFilters(GenericExecuteSpec spec, Long courseId, String keyword,
                                                  Integer type, Integer difficulty, Long nodeId) {
        if (courseId != null) {
            spec = spec.bind("courseId", courseId);
        }
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.bind("kw", "%" + keyword.trim() + "%");
        }
        if (type != null) {
            spec = spec.bind("type", type);
        }
        if (difficulty != null) {
            spec = spec.bind("difficulty", difficulty);
        }
        if (nodeId != null) {
            spec = spec.bind("nodeId", nodeId);
        }
        return spec;
    }

    private static Question mapQuestion(Row row, RowMetadata meta) {
        return Question.builder()
                .id(row.get("id", Long.class))
                .courseId(row.get("course_id", Long.class))
                .type(row.get("type", Integer.class))
                .stem(row.get("stem", String.class))
                .answer(row.get("answer", String.class))
                .analysis(row.get("analysis", String.class))
                .difficulty(row.get("difficulty", Integer.class))
                .nodeId(row.get("node_id", Long.class))
                .lang(row.get("lang", String.class))
                .createdBy(row.get("created_by", Long.class))
                .createdAt(row.get("created_at", LocalDateTime.class))
                .updatedAt(row.get("updated_at", LocalDateTime.class))
                .deleted(row.get("deleted", Integer.class))
                .build();
    }
}
